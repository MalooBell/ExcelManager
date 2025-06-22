package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
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
    private final FileEntityRepository fileRepository;
    private final ObjectMapper objectMapper;

    @Value("${excel.upload.max-file-size:10485760}")
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}")
    private int maxRows;
    private static final int HEADER_DETECTION_MIN_COLUMNS = 3;


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

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity = fileRepository.save(fileEntity);

        List<String> errors = new ArrayList<>();
        AtomicInteger totalProcessedRows = new AtomicInteger(0);

        try (InputStream inputStream = file.getInputStream()) {
            // ÉTAPE 1 - ANALYSE DES EN-TÊTES
            HeaderAnalysisListener headerAnalysisListener = new HeaderAnalysisListener(HEADER_DETECTION_MIN_COLUMNS);
            
            try (InputStream analysisStream = file.getInputStream()) {
                 EasyExcel.read(analysisStream, headerAnalysisListener).sheet(0).doRead();
            } catch (Exception e) {
                 if (e.getMessage() == null || !e.getMessage().contains("interrupt")) {
                     throw e;
                 }
            }

            List<String> headers = headerAnalysisListener.getHeaders();
            int headerRowNumber = headerAnalysisListener.getHeaderRowNumber(); // 1-based

            if (headerRowNumber == -1) {
                fileRepository.delete(fileEntity);
                return new UploadResponse(false, "Impossible de détecter une ligne d'en-tête valide dans le fichier.", null, 0);
            }
            
            logger.info("Header detected on row {} with {} columns.", headerRowNumber, headers.size());
            fileEntity.setHeadersJson(objectMapper.writeValueAsString(headers));
            
            // ÉTAPE 2 - LECTURE DES DONNÉES
            // CORRECTION : Nous passons le numéro de la ligne d'en-tête au listener de données
            ExcelDataListener dataListener = new ExcelDataListener(fileEntity, 0, totalProcessedRows, errors, headers, maxRows, headerRowNumber);
            
            // CORRECTION : On ne spécifie PLUS .headRowNumber() ici. On lit tout et on laisse le listener ignorer les premières lignes.
            EasyExcel.read(inputStream, dataListener)
                    .sheet(0)
                    .doRead();
            
            fileEntity.setTotalRows(totalProcessedRows.get());
            fileRepository.save(fileEntity);

            String message = String.format("Processing finished. %d rows processed.", totalProcessedRows.get());
            if (!errors.isEmpty()) {
                message += " with " + errors.size() + " error(s).";
            }
            return new UploadResponse(true, message, errors.isEmpty() ? null : errors, totalProcessedRows.get());

        } catch (Exception e) {
            logger.error("General error during file processing: {}", e.getMessage(), e);
            fileRepository.deleteById(fileEntity.getId());
            return new UploadResponse(false, "Error during processing: " + e.getMessage(), null, 0);
        }
    }

    private static class HeaderAnalysisListener implements ReadListener<Map<Integer, String>> {
        private List<String> headers = new ArrayList<>();
        private int headerRowNumber = -1;
        private final int minColumns;

        public HeaderAnalysisListener(int minColumns) {
            this.minColumns = minColumns;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            long nonEmptyCells = data.values().stream().filter(cell -> cell != null && !cell.trim().isEmpty()).count();

            if (nonEmptyCells >= minColumns) {
                this.headerRowNumber = context.readRowHolder().getRowIndex() + 1;

                int maxColumnIndex = data.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                for (int i = 0; i <= maxColumnIndex; i++) {
                    String cellValue = data.get(i);
                    headers.add(cellValue == null ? "" : cellValue.trim());
                }

                while (!headers.isEmpty() && headers.get(headers.size() - 1).isEmpty()) {
                    headers.remove(headers.size() - 1);
                }
                
                context.interrupt();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
        public List<String> getHeaders() { return headers; }
        public int getHeaderRowNumber() { return headerRowNumber; }
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
        
        // CORRECTION : Ajout d'un champ pour savoir où commencer à lire
        private final int headerRowIndex; // 0-based index

        public ExcelDataListener(FileEntity fileEntity, int sheetIndex, AtomicInteger totalProcessedRows, List<String> errors, List<String> headers, int maxRows, int headerRowNumber) {
            this.fileEntity = fileEntity;
            this.sheetIndex = sheetIndex;
            this.totalProcessedRows = totalProcessedRows;
            this.errors = errors;
            this.headers = headers;
            this.maxRows = maxRows;
            // CORRECTION : On stocke l'index (0-based) de la ligne d'en-tête
            this.headerRowIndex = headerRowNumber - 1;
        }

        // Dans la classe interne ExcelDataListener
@Override
public void invoke(Map<Integer, String> data, AnalysisContext context) {
    // On ignore manuellement toutes les lignes situées avant ou sur la ligne d'en-tête
    if (context.readRowHolder().getRowIndex() <= this.headerRowIndex) {
        return;
    }
    
    if (totalProcessedRows.get() >= maxRows) {
        if(errors.stream().noneMatch(e -> e.startsWith("Row limit exceeded"))) {
            errors.add("Row limit exceeded. Max rows: " + maxRows);
        }
        context.interrupt();
        return;
    }

    try {
        // On ignore les lignes complètement vides
        if (data.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
            return;
        }
        
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String columnName = headers.get(i);
            String cellValue = data.get(i); // Récupère la valeur, qui peut être null

            // CORRECTION DÉFINITIVE : On vérifie si la valeur est null AVANT d'appeler .trim()
            // Si la cellule est null, on insère null. Sinon, on insère la valeur nettoyée.
            rowData.put(columnName, cellValue == null ? null : cellValue.trim());
        }

        String jsonData = objectMapper.writeValueAsString(rowData);
        RowEntity entity = RowEntity.builder()
                .dataJson(jsonData)
                .sheetIndex(sheetIndex)
                .file(fileEntity)
                .build();

        batchData.add(entity);
        totalProcessedRows.incrementAndGet();

        if (batchData.size() >= BATCH_SIZE) {
            saveBatch();
        }
    } catch (Exception e) {
        int rowIndex = context.readRowHolder().getRowIndex() + 1;
        logger.error("Error on row {}: {}", rowIndex, e.getMessage(), e);
        errors.add("Row " + rowIndex + ": " + e.getMessage());
    }
}

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchData.isEmpty()) {
                saveBatch();
            }
            logger.info("Sheet {} processed. Total rows saved: {}", sheetIndex, totalProcessedRows.get());
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