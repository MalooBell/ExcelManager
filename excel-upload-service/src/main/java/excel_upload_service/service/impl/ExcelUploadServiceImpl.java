// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/ExcelUploadServiceImpl.java
package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.service.ExcelUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);
    private final RowEntityRepository rowRepository;
    private final FileEntityRepository fileRepository;
    private final SheetEntityRepository sheetRepository;
    private final ObjectMapper objectMapper;

    @Value("${excel.upload.max-file-size:10485760}")
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}")
    private int maxRows;

    public ExcelUploadServiceImpl(RowEntityRepository rowRepository, FileEntityRepository fileRepository,
                                  SheetEntityRepository sheetRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.fileRepository = fileRepository;
        this.sheetRepository = sheetRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UploadResponse uploadAndProcessExcel(MultipartFile file) {
        logger.info("Starting processing for file: {}", file.getOriginalFilename());
        if (file.getSize() > maxFileSize) {
            return new UploadResponse(false, "File is too large. Max allowed size: " + (maxFileSize / 1024 / 1024) + "MB", null, 0);
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity = fileRepository.save(fileEntity);

        List<String> errors = new ArrayList<>();
        AtomicInteger totalProcessedRowsInFile = new AtomicInteger(0);

        try {
            List<com.alibaba.excel.read.metadata.ReadSheet> sheetMetadatas;
            try (InputStream is = file.getInputStream()) {
                sheetMetadatas = EasyExcel.read(is).build().excelExecutor().sheetList();
            }

            if (sheetMetadatas.isEmpty()) {
                return new UploadResponse(false, "The Excel file contains no sheets.", null, 0);
            }

            for (com.alibaba.excel.read.metadata.ReadSheet sheetInfo : sheetMetadatas) {
                int sheetIndex = sheetInfo.getSheetNo();
                String sheetName = sheetInfo.getSheetName();

                try (InputStream inputStream = file.getInputStream()) {
                    List<String> headers = extractHeaders(file, sheetIndex);
                    if (headers.isEmpty() || headers.stream().allMatch(h -> h == null || h.trim().isEmpty())) {
                        logger.warn("Sheet '{}' (index {}) has no valid headers, skipping.", sheetName, sheetIndex);
                        continue;
                    }

                    SheetEntity sheetEntity = new SheetEntity();
                    sheetEntity.setFile(fileEntity);
                    sheetEntity.setSheetIndex(sheetIndex);
                    sheetEntity.setSheetName(sheetName);
                    sheetEntity.setHeadersJson(objectMapper.writeValueAsString(headers));
                    sheetEntity = sheetRepository.save(sheetEntity);

                    AtomicInteger processedRowsInSheet = new AtomicInteger(0);
                    ExcelDataListener dataListener = new ExcelDataListener(sheetEntity, headers, processedRowsInSheet, errors, maxRows, totalProcessedRowsInFile);

                    EasyExcel.read(inputStream, dataListener)
                            .sheet(sheetIndex)
                            .headRowNumber(1)
                            .doRead();

                    sheetEntity.setTotalRows(processedRowsInSheet.get());
                    sheetRepository.save(sheetEntity);

                } catch (Exception e) {
                    logger.error("Error processing sheet '{}': {}", sheetName, e.getMessage(), e);
                    errors.add("Feuille '" + sheetName + "': " + e.getMessage());
                }
            }

            String message = String.format("Processing finished. %d rows processed across %d sheets.", totalProcessedRowsInFile.get(), sheetMetadatas.size());
            if (!errors.isEmpty()) {
                message += " with " + errors.size() + " error(s).";
            }
            return new UploadResponse(true, message, errors.isEmpty() ? null : errors, totalProcessedRowsInFile.get());

        } catch (Exception e) {
            logger.error("General error during file processing: {}", e.getMessage(), e);
            fileRepository.deleteById(fileEntity.getId());
            return new UploadResponse(false, "Error during processing: " + e.getMessage(), null, 0);
        }
    }
    
    private List<String> extractHeaders(MultipartFile file, int sheetIndex) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            HeaderListener headerListener = new HeaderListener();
            try {
                EasyExcel.read(inputStream, headerListener)
                        .sheet(sheetIndex)
                        .headRowNumber(1)
                        .doRead();
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                    logger.debug("Header read interrupted as expected for sheet {}.", sheetIndex);
                } else {
                    throw e;
                }
            }
            return headerListener.getHeaders();
        }
    }

    private static class HeaderListener implements ReadListener<Map<Integer, String>> {
        private final List<String> headers = new ArrayList<>();
        public List<String> getHeaders() { return headers; }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (headMap != null && !headMap.isEmpty()) {
                for (ReadCellData<?> cell : headMap.values()) {
                    if (cell != null && cell.getStringValue() != null) {
                        headers.add(cell.getStringValue());
                    }
                }
            }
            context.interrupt();
        }
        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {}
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
    }

    private class ExcelDataListener implements ReadListener<Map<Integer, String>> {
        private final SheetEntity sheetEntity;
        private final List<String> headers;
        private final AtomicInteger processedRowsInSheet;
        private final AtomicInteger totalProcessedRowsInFile;
        private final List<String> errors;
        private final int maxRows;
        private final List<RowEntity> batchData = new ArrayList<>();
        private static final int BATCH_SIZE = 1000;

        public ExcelDataListener(SheetEntity sheetEntity, List<String> headers, AtomicInteger processedRowsInSheet,
                                 List<String> errors, int maxRows, AtomicInteger totalProcessedRowsInFile) {
            this.sheetEntity = sheetEntity;
            this.headers = headers;
            this.processedRowsInSheet = processedRowsInSheet;
            this.errors = errors;
            this.maxRows = maxRows;
            this.totalProcessedRowsInFile = totalProcessedRowsInFile;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            try {
                if (totalProcessedRowsInFile.get() >= maxRows) {
                     context.interrupt();
                     if(errors.stream().noneMatch(e -> e.contains("Row limit exceeded"))) {
                        errors.add("Row limit exceeded: " + maxRows);
                     }
                     return;
                }

                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String columnName = headers.get(i);
                    String value = data.get(i);
                    if (value != null && !value.trim().isEmpty()) {
                        rowData.put(columnName, value.trim());
                    }
                }

                if (rowData.isEmpty() || rowData.values().stream().allMatch(Objects::isNull)) {
                    return;
                }
                
                String jsonData = objectMapper.writeValueAsString(rowData);
                
                // CORRECTION : On passe l'objet sheetEntity complet, pas l'index.
                RowEntity entity = RowEntity.builder()
                        .dataJson(jsonData)
                        .sheet(this.sheetEntity)
                        .build();

                batchData.add(entity);
                processedRowsInSheet.incrementAndGet();
                totalProcessedRowsInFile.incrementAndGet();

                if (batchData.size() >= BATCH_SIZE) {
                    saveBatch();
                }

            } catch (Exception e) {
                int rowIndex = context.readRowHolder().getRowIndex();
                logger.error("Error on sheet '{}' row {}: {}", sheetEntity.getSheetName(), rowIndex, e.getMessage());
                errors.add("Sheet " + sheetEntity.getSheetName() + " Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchData.isEmpty()) {
                saveBatch();
            }
            logger.info("Sheet '{}' processed: {} rows.", sheetEntity.getSheetName(), processedRowsInSheet.get());
        }

        private void saveBatch() {
            try {
                rowRepository.saveAll(batchData);
                batchData.clear();
            } catch (Exception e) {
                logger.error("Error saving batch: {}", e.getMessage());
                errors.add("Database save error: " + e.getMessage());
            }
        }
    }
}