package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.RowEntityRepository;
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
    private final FileEntityRepository fileRepository; // New repository injected
    private final ObjectMapper objectMapper;

    @Value("${excel.upload.max-file-size:10485760}") // 10MB
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}") // 1,000,700 rows
    private int maxRows;

    public ExcelUploadServiceImpl(RowEntityRepository rowRepository, FileEntityRepository fileRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.fileRepository = fileRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UploadResponse uploadAndProcessExcel(MultipartFile file) {
        logger.info("Starting processing for file: {}", file.getOriginalFilename());
        if (file.getSize() > maxFileSize) {
            return new UploadResponse(false, "File is too large. Max allowed size: " + (maxFileSize / 1024 / 1024) + "MB", null, 0);
        }

        // --- Create and save the FileEntity first ---
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity = fileRepository.save(fileEntity); // Save to get the ID

        List<String> errors = new ArrayList<>();
        AtomicInteger totalProcessedRows = new AtomicInteger(0);

        try (InputStream inputStream = file.getInputStream()) {
            List<com.alibaba.excel.read.metadata.ReadSheet> sheets = EasyExcel.read(inputStream).build().excelExecutor().sheetList();
            if (sheets.isEmpty()) {
                return new UploadResponse(false, "The Excel file contains no sheets.", null, 0);
            }

            // --- Extract headers from the first sheet to store in FileEntity ---
            List<String> headers = extractHeaders(file, 0);
            fileEntity.setHeadersJson(objectMapper.writeValueAsString(headers));

            for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
                try (InputStream sheetInputStream = file.getInputStream()) {
                    ExcelDataListener dataListener = new ExcelDataListener(fileEntity, sheetIndex, totalProcessedRows, errors, headers, maxRows);
                    EasyExcel.read(sheetInputStream, dataListener)
                            .sheet(sheetIndex)
                            .headRowNumber(1) // Assuming headers are on the first row
                            .doRead();
                } catch (Exception e) {
                    logger.error("Unexpected error processing sheet {}: {}", sheetIndex, e.getMessage(), e);
                    errors.add("Sheet " + (sheetIndex + 1) + ": " + e.getMessage());
                }
            }

            // Finalize FileEntity
            fileEntity.setTotalRows(totalProcessedRows.get());
            fileRepository.save(fileEntity);

            String message = String.format("Processing finished. %d rows processed across %d sheets.", totalProcessedRows.get(), sheets.size());
            if (!errors.isEmpty()) {
                message += " with " + errors.size() + " error(s).";
            }
            return new UploadResponse(true, message, errors.isEmpty() ? null : errors, totalProcessedRows.get());

        } catch (Exception e) {
            logger.error("General error during file processing: {}", e.getMessage(), e);
            // If something fails, delete the created FileEntity to avoid orphans
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

    // --- INNER LISTENER CLASSES ---

    private static class HeaderListener implements ReadListener<Map<Integer, String>> {
        private final List<String> headers = new ArrayList<>();
        public List<String> getHeaders() { return headers; }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (headMap != null && !headMap.isEmpty()) {
                headMap.values().forEach(cell -> {
                    if (cell != null && cell.getStringValue() != null) {
                        headers.add(cell.getStringValue());
                    }
                });
            }
            context.interrupt(); // Stop reading after headers
        }
        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {}
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
    }

    private class ExcelDataListener implements ReadListener<Map<Integer, String>> {
        private final FileEntity fileEntity;
        private final int sheetIndex;
        private final AtomicInteger totalProcessedRows;
        private final List<String> errors;
        private final List<String> headers;
        private final int maxRows;
        private final List<RowEntity> batchData = new ArrayList<>();
        private static final int BATCH_SIZE = 1000;

        public ExcelDataListener(FileEntity fileEntity, int sheetIndex, AtomicInteger totalProcessedRows, List<String> errors, List<String> headers, int maxRows) {
            this.fileEntity = fileEntity;
            this.sheetIndex = sheetIndex;
            this.totalProcessedRows = totalProcessedRows;
            this.errors = errors;
            this.headers = headers.isEmpty() ? new ArrayList<>() : headers;
            this.maxRows = maxRows;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            try {
                if (totalProcessedRows.get() >= maxRows) {
                    throw new RuntimeException("Row limit exceeded: " + maxRows);
                }

                Map<String, Object> rowData = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
                for (int i = 0; i < headers.size(); i++) {
                    String columnName = headers.get(i);
                    String value = data.get(i);
                    if (value != null && !value.trim().isEmpty()) {
                        rowData.put(columnName, value.trim());
                    }
                }

                if (rowData.values().stream().allMatch(Objects::isNull)) {
                    return; // Skip empty rows
                }

                String jsonData = objectMapper.writeValueAsString(rowData);
                RowEntity entity = RowEntity.builder()
                        .dataJson(jsonData)
                        .sheetIndex(sheetIndex)
                        .file(fileEntity) // Associate with the file
                        .build();

                batchData.add(entity);
                totalProcessedRows.incrementAndGet();

                if (batchData.size() >= BATCH_SIZE) {
                    saveBatch();
                }

            } catch (Exception e) {
                int rowIndex = context.readRowHolder().getRowIndex();
                logger.error("Error on row {}: {}", rowIndex, e.getMessage());
                errors.add("Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchData.isEmpty()) {
                saveBatch();
            }
            logger.info("Sheet {} processed: {} rows.", sheetIndex, context.readRowHolder().getRowIndex());
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