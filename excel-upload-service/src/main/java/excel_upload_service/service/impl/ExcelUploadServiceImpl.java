package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
// L'IMPORT DE L'EXCEPTION A ÉTÉ RETIRÉ
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.model.RowEntity;
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

@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);
    private final RowEntityRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${excel.upload.max-file-size:10485760}") // 10MB par défaut
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}") // 10000 lignes par défaut
    private int maxRows;

    public ExcelUploadServiceImpl(RowEntityRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    private static class HeaderListener implements ReadListener<Map<Integer, String>> {
        private final List<String> headers = new ArrayList<>();

        public List<String> getHeaders() {
            return headers;
        }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (headMap != null && !headMap.isEmpty()) {
                headMap.values().forEach(cell -> {
                    if (cell != null) {
                        headers.add(cell.getStringValue());
                    }
                });
            }
            context.interrupt();
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) { }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) { }
    }


    @Override
    @Transactional
    public UploadResponse uploadAndProcessExcel(MultipartFile file) {
        logger.info("Début du traitement du fichier: {}", file.getOriginalFilename());
        List<String> errors = new ArrayList<>();
        var totalProcessedRows = new java.util.concurrent.atomic.AtomicInteger(0);
        try {
            if (file.getSize() > maxFileSize) {
                return new UploadResponse(false, "Le fichier est trop volumineux. Taille maximale autorisée: " + (maxFileSize / 1024 / 1024) + "MB", null, 0);
            }

            List<com.alibaba.excel.read.metadata.ReadSheet> sheets = EasyExcel.read(file.getInputStream()).build().excelExecutor().sheetList();
            if (sheets.isEmpty()) {
                return new UploadResponse(false, "Le fichier Excel ne contient aucune feuille", null, 0);
            }

            for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
                try {
                    processSheet(file, sheetIndex, totalProcessedRows, errors);
                } catch (Exception e) {
                    logger.error("Erreur inattendue lors du traitement de la feuille {}: {}", sheetIndex, e.getMessage(), e);
                    errors.add("Feuille " + (sheetIndex + 1) + ": " + e.getMessage());
                }
            }

            String message = String.format("Traitement terminé. %d lignes traitées sur %d feuilles", totalProcessedRows.get(), sheets.size());
            if (!errors.isEmpty()) {
                message += " avec " + errors.size() + " erreur(s)";
            }

            return new UploadResponse(true, message, errors.isEmpty() ? null : errors, totalProcessedRows.get());
        } catch (Exception e) {
            logger.error("Erreur générale lors du traitement du fichier: {}", e.getMessage(), e);
            return new UploadResponse(false, "Erreur lors du traitement: " + e.getMessage(), null, 0);
        }
    }

    // *** DERNIÈRE MODIFICATION DANS LE BLOC CATCH ***
    private void processSheet(MultipartFile file, int sheetIndex, java.util.concurrent.atomic.AtomicInteger totalProcessedRows, List<String> errors) throws IOException {
        String fileName = file.getOriginalFilename();

        List<String> headers;
        try (InputStream inputStream = file.getInputStream()) {
            HeaderListener headerListener = new HeaderListener();
            try {
                EasyExcel.read(inputStream, headerListener)
                        .sheet(sheetIndex)
                        .headRowNumber(1)
                        .doRead();
            } catch (RuntimeException e) {
                // On attrape une exception générale et on vérifie si c'est bien l'interruption attendue
                if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                    logger.debug("Interruption de la lecture des en-têtes terminée comme attendu pour la feuille {}.", sheetIndex);
                } else {
                    // Si ce n'est pas le cas, c'est une autre erreur, donc on la relance
                    throw e;
                }
            }
            headers = headerListener.getHeaders();
        }

        if (headers.isEmpty()) {
            logger.warn("Aucun en-tête trouvé pour la feuille {}, des en-têtes génériques seront utilisés.", sheetIndex);
        }

        ExcelDataListener dataListener = new ExcelDataListener(fileName, sheetIndex, totalProcessedRows, errors, headers);

        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, dataListener)
                    .sheet(sheetIndex)
                    .headRowNumber(1)
                    .doRead();
        }
    }

    private class ExcelDataListener implements ReadListener<Map<Integer, String>> {

        private final String fileName;
        private final int sheetIndex;
        private final java.util.concurrent.atomic.AtomicInteger totalProcessedRows;
        private final List<String> errors;
        private final List<RowEntity> batchData = new ArrayList<>();
        private final int BATCH_SIZE = 10000;
        private List<String> headers;
        private boolean headersInitialized = false;

        public ExcelDataListener(String fileName, int sheetIndex, java.util.concurrent.atomic.AtomicInteger totalProcessedRows, List<String> errors, List<String> headers) {
            this.fileName = fileName;
            this.sheetIndex = sheetIndex;
            this.totalProcessedRows = totalProcessedRows;
            this.errors = errors;
            this.headers = headers;
            this.headersInitialized = !headers.isEmpty();
        }

        private void initializeGenericHeaders(Map<Integer, String> data) {
            if (!headersInitialized) {
                headers = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    headers.add("Colonne_" + (i + 1));
                }
                headersInitialized = true;
                logger.debug("En-têtes génériques initialisés pour la feuille {}: {} colonnes", sheetIndex, headers.size());
            }
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            try {
                if (!headersInitialized) {
                    initializeGenericHeaders(data);
                }

                if (totalProcessedRows.get() >= maxRows) {
                    throw new RuntimeException("Limite de lignes dépassée: " + maxRows);
                }

                Map<String, Object> rowData = new HashMap<>();
                for (Map.Entry<Integer, String> entry : data.entrySet()) {
                    Integer columnIndex = entry.getKey();
                    String value = entry.getValue();

                    if (columnIndex < headers.size()) {
                        String columnName = headers.get(columnIndex);
                        if (value != null && !value.trim().isEmpty()) {
                            rowData.put(columnName, value.trim());
                        }
                    }
                }

                if (rowData.isEmpty()) {
                    return;
                }

                String jsonData = objectMapper.writeValueAsString(rowData);
                RowEntity entity = RowEntity.builder()
                        .dataJson(jsonData)
                        .sheetIndex(sheetIndex)
                        .fileName(fileName)
                        .build();

                batchData.add(entity);
                totalProcessedRows.incrementAndGet();
                if (batchData.size() >= BATCH_SIZE) {
                    saveBatch();
                }

            } catch (JsonProcessingException e) {
                logger.error("Erreur de sérialisation JSON ligne {}: {}", context.readRowHolder().getRowIndex(), e.getMessage());
                errors.add("Ligne " + (context.readRowHolder().getRowIndex() + 1) + ": Erreur de format de données");
            } catch (Exception e) {
                logger.error("Erreur ligne {}: {}", context.readRowHolder().getRowIndex(), e.getMessage());
                errors.add("Ligne " + (context.readRowHolder().getRowIndex() + 1) + ": " + e.getMessage());
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchData.isEmpty()) {
                saveBatch();
            }
            logger.info("Feuille {} traitée: {} lignes", sheetIndex, context.readRowHolder().getRowIndex());
        }

        private void saveBatch() {
            try {
                repository.saveAll(batchData);
                logger.debug("Batch de {} lignes sauvegardé", batchData.size());
                batchData.clear();
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde du batch: {}", e.getMessage());
                errors.add("Erreur de sauvegarde: " + e.getMessage());
            }
        }
    }
}