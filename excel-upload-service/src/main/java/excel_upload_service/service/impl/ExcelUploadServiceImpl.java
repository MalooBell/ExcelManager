// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/ExcelUploadServiceImpl.java
package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.LayoutAnalysis;
import excel_upload_service.dto.SheetMappingDto;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.model.SheetMapping;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.ModificationHistoryRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.repository.SheetMappingRepository;
import excel_upload_service.service.ExcelUploadService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);
    private final RowEntityRepository rowRepository;
    private final FileEntityRepository fileRepository;
    private final SheetEntityRepository sheetRepository;
    private final SheetMappingRepository sheetMappingRepository;
    private final ObjectMapper objectMapper;
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private final Path fileStorageLocation;
    private final ModificationHistoryRepository historyRepository;


    @Value("${excel.upload.max-file-size:10485760}")
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}")
    private int maxRows;

    public ExcelUploadServiceImpl(RowEntityRepository rowRepository, FileEntityRepository fileRepository,
                                  SheetEntityRepository sheetRepository, SheetMappingRepository sheetMappingRepository,
                                  ObjectMapper objectMapper, ModificationHistoryRepository historyRepository) {
        this.rowRepository = rowRepository;
        this.fileRepository = fileRepository;
        this.sheetRepository = sheetRepository;
        this.sheetMappingRepository = sheetMappingRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage des fichiers.", ex);
        }
    }

    @Override
    @Transactional
    public UploadResponse uploadAndProcessExcel(MultipartFile file) {
        logger.info("Starting initial upload for file: {}", file.getOriginalFilename());
        if (file.getSize() > maxFileSize) {
            return new UploadResponse(false, "File is too large. Max allowed size: " + (maxFileSize / 1024 / 1024) + "MB", null);
        }

        String uniqueFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(uniqueFileName);
        fileEntity = fileRepository.save(fileEntity);

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFileName, ex);
        }

        boolean needsValidation = false;
        try (InputStream inputStream = file.getInputStream()) {
            List<com.alibaba.excel.read.metadata.ReadSheet> sheetMetadatas = EasyExcel.read(inputStream).build().excelExecutor().sheetList();
            if (sheetMetadatas.isEmpty()) {
                return new UploadResponse(false, "The Excel file contains no sheets.", null);
            }

            // On ne vérifie que la première feuille pour décider du flux de travail.
            com.alibaba.excel.read.metadata.ReadSheet firstSheet = sheetMetadatas.get(0);
            try(InputStream layoutStream = file.getInputStream()) {
                LayoutAnalysis layout = detectLayout(layoutStream, firstSheet.getSheetNo());
                if (!layout.isReliable()) {
                    needsValidation = true;
                }
                
                // On importe immédiatement les données SI la détection est fiable.
                if(!needsValidation) {
                    processAllSheets(file, fileEntity);
                }
            }

        } catch (IOException e) {
            logger.error("Error reading file during initial check", e);
            return new UploadResponse(false, "Error reading file: " + e.getMessage(), null);
        }

        String message = needsValidation ? "File uploaded, manual validation required." : "File uploaded and processed successfully.";
        return new UploadResponse(true, message, fileEntity.getId(), needsValidation);
    }
    
    /**
     * NOUVEAU : Méthode qui contient l'ancienne logique de traitement de toutes les feuilles.
     * Elle sera appelée si la détection automatique est un succès.
     */
    private void processAllSheets(MultipartFile file, FileEntity fileEntity) throws IOException {
         try (InputStream is = file.getInputStream()) {
                List<com.alibaba.excel.read.metadata.ReadSheet> sheetMetadatas = EasyExcel.read(is).build().excelExecutor().sheetList();
                for (com.alibaba.excel.read.metadata.ReadSheet sheetInfo : sheetMetadatas) {
                     try(InputStream sheetStream = file.getInputStream()){
                         LayoutAnalysis layout = detectLayout(sheetStream, sheetInfo.getSheetNo());
                         try(InputStream processStream = file.getInputStream()){
                             processSheet(processStream, sheetInfo, layout, fileEntity, new AtomicInteger(0), new ArrayList<>());
                         }
                     }
                }
            }
    }
    
   private void processSheet(InputStream fileStream, com.alibaba.excel.read.metadata.ReadSheet sheetInfo, LayoutAnalysis layout, FileEntity fileEntity, AtomicInteger totalProcessedRowsInFile, List<String> errors) throws IOException {
        int sheetIndex = sheetInfo.getSheetNo();
        String sheetName = sheetInfo.getSheetName();

        List<String> headers;
        // On utilise la nouvelle méthode `getStoredFileStream` pour relire le fichier stocké.
        try (InputStream headerStream = fileEntity.getStoredFileStream(fileStorageLocation)) {
            headers = extractHeaders(headerStream, sheetIndex, layout.getHeaderRowIndex());
        }

        if (headers.isEmpty() || headers.stream().allMatch(h -> h == null || h.trim().isEmpty())) {
            logger.warn("Sheet '{}' (index {}) has no valid headers based on detected layout, skipping.", sheetName, sheetIndex);
            return;
        }

        SheetEntity sheetEntity = new SheetEntity();
        sheetEntity.setFile(fileEntity);
        sheetEntity.setSheetIndex(sheetIndex);
        sheetEntity.setSheetName(sheetName);
        sheetEntity.setHeadersJson(objectMapper.writeValueAsString(headers));
        sheetEntity = sheetRepository.save(sheetEntity);
        
        Optional<SheetMapping> mapping = sheetMappingRepository.findBySheetId(sheetEntity.getId());
        if (mapping.isPresent()) {
            logger.info("Mapping found for sheet ID {}", sheetEntity.getId());
        } else {
            logger.info("No mapping found for sheet ID {}. Proceeding with direct import.", sheetEntity.getId());
        }

        AtomicInteger processedRowsInSheet = new AtomicInteger(0);
        ExcelDataListener dataListener = new ExcelDataListener(sheetEntity, headers, processedRowsInSheet, errors, maxRows, totalProcessedRowsInFile, layout.getHeaderRowIndex(), mapping);

        EasyExcel.read(fileStream, dataListener)
            .sheet(sheetIndex)
            .headRowNumber(0)
            .doRead();

        sheetEntity.setTotalRows(processedRowsInSheet.get());
        sheetRepository.save(sheetEntity);
    }

    

    // private List<String> extractHeaders(MultipartFile file, int sheetIndex, int headRowNumber) throws IOException {
    //     try (InputStream inputStream = file.getInputStream()) {
    //         HeaderListener headerListener = new HeaderListener();
    //         try {
    //             EasyExcel.read(inputStream, headerListener)
    //                     .sheet(sheetIndex)
    //                     .headRowNumber(headRowNumber)
    //                     .doRead();
    //         } catch (RuntimeException e) {
    //             if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
    //                 logger.debug("Header read interrupted as expected for sheet {}.", sheetIndex);
    //             } else {
    //                 throw e;
    //             }
    //         }
    //         return headerListener.getHeaders();
    //     }
    // }

     private List<String> extractHeaders(InputStream inputStream, int sheetIndex, int headRowNumber) {
        HeaderListener headerListener = new HeaderListener();
         try {
            EasyExcel.read(inputStream, headerListener)
                    .sheet(sheetIndex)
                    .headRowNumber(headRowNumber)
                    .doRead();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                // Comportement attendu
            } else {
                throw e;
            }
        }
        return headerListener.getHeaders();
    }

    private static class HeaderListener implements ReadListener<Map<Integer, String>> {
        private final List<String> headers = new ArrayList<>();
        public List<String> getHeaders() { return headers; }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (headMap != null && !headMap.isEmpty()) {
                List<Integer> sortedKeys = new ArrayList<>(headMap.keySet());
                Collections.sort(sortedKeys);
                for (Integer key : sortedKeys) {
                    ReadCellData<?> cell = headMap.get(key);
                    String cellValue = (cell != null) ? cell.getStringValue() : null;
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        headers.add(cellValue.trim());
                    } else {
                        headers.add(null);
                    }
                }
                while (headers.size() > 0 && headers.get(headers.size() - 1) == null) {
                    headers.remove(headers.size() - 1);
                }
            }
            context.interrupt();
        }
        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {}
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
    }

    private static class LayoutDetectionListener implements ReadListener<Map<Integer, String>> {
        private static final int ROW_LIMIT = 20;
        private final List<Map<Integer, String>> rows = new ArrayList<>();

        public List<Map<Integer, String>> getRows() {
            return rows;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            rows.add(new HashMap<>(data));
            if (rows.size() >= ROW_LIMIT) {
                context.interrupt();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}
    }
    
    /**
     * MODIFICATION FINALE : Le listener a été simplifié pour n'accepter que Map<Integer, String>.
     * C'est ce qu'EasyExcel fournit par défaut et cela résout l'erreur de ClassCastException.
     * La logique de gestion des fusions est conservée mais adaptée à ce type de données plus simple.
     */
    private class ExcelDataListener implements ReadListener<Map<Integer, String>> {
        private final SheetEntity sheetEntity;
        private final List<String> headers;
        private final AtomicInteger processedRowsInSheet;
        private final AtomicInteger totalProcessedRowsInFile;
        private final List<String> errors;
        private final int maxRows;
        private final List<RowEntity> batchData = new ArrayList<>();
        private static final int BATCH_SIZE = 1000;
        private final int headerRowIndex;
        private final Optional<SheetMappingDto> mappingDto;
        private final Map<String, String> sourceToDestinationMap = new HashMap<>();
        private final boolean hasMapping;
        private final Map<Integer, String> lastRowData = new HashMap<>();

        public ExcelDataListener(SheetEntity sheetEntity, List<String> headers, AtomicInteger processedRowsInSheet,
                                 List<String> errors, int maxRows, AtomicInteger totalProcessedRowsInFile, int headRowNumber,
                                 Optional<SheetMapping> sheetMappingOpt) {
            this.sheetEntity = sheetEntity;
            this.headers = headers;
            this.processedRowsInSheet = processedRowsInSheet;
            this.errors = errors;
            this.maxRows = maxRows;
            this.totalProcessedRowsInFile = totalProcessedRowsInFile;
            this.headerRowIndex = headRowNumber;

            this.hasMapping = sheetMappingOpt.isPresent();
            if (this.hasMapping) {
                try {
                    this.mappingDto = Optional.of(objectMapper.readValue(sheetMappingOpt.get().getMappingDefinitionJson(), SheetMappingDto.class));
                    this.mappingDto.ifPresent(dto -> {
                        dto.getMappings().forEach(mapRule -> {
                            sourceToDestinationMap.put(mapRule.get("source"), mapRule.get("destination"));
                        });
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse mapping JSON", e);
                }
            } else {
                this.mappingDto = Optional.empty();
            }
        }

        // MODIFIÉ : La signature de `invoke` accepte maintenant Map<Integer, String>
        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            int currentRowIndex = context.readRowHolder().getRowIndex() + 1;
            if (currentRowIndex <= this.headerRowIndex) {
                return;
            }

            try {
                if (totalProcessedRowsInFile.get() >= maxRows) {
                     context.interrupt();
                     if(errors.stream().noneMatch(e -> e.contains("Row limit exceeded"))) {
                        errors.add("Row limit exceeded: " + maxRows);
                     }
                     return;
                }

                // La logique de fusion est maintenant appliquée sur la Map<Integer, String>
                Map<Integer, String> filledData = fillMergedCells(data);

                Map<String, Object> rowData;
                if (hasMapping) {
                    rowData = processRowWithMapping(filledData);
                } else {
                    rowData = processRowWithoutMapping(filledData);
                }

                if (rowData.isEmpty()) {
                    return;
                }

                String jsonData = objectMapper.writeValueAsString(rowData);

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
                logger.error("Error on sheet '{}' row {}: {}", sheetEntity.getSheetName(), rowIndex, e.getMessage(), e);
                errors.add("Sheet " + sheetEntity.getSheetName() + " Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }
        
        // MODIFIÉ : Prend et retourne une Map<Integer, String>
        private Map<String, Object> processRowWithMapping(Map<Integer, String> data) {
            Map<String, Object> rowData = new LinkedHashMap<>();
            boolean ignoreUnmapped = mappingDto.map(SheetMappingDto::isIgnoreUnmapped).orElse(false);

            for (int i = 0; i < headers.size(); i++) {
                String sourceHeader = headers.get(i);
                if (sourceHeader == null) continue;

                String value = data.get(i);
                if (value == null || value.trim().isEmpty()) continue;

                String destinationHeader = sourceToDestinationMap.get(sourceHeader);

                if (destinationHeader != null) {
                    rowData.put(destinationHeader, value.trim());
                } else if (!ignoreUnmapped) {
                    rowData.put(sourceHeader, value.trim());
                }
            }
            return rowData;
        }

        // MODIFIÉ : Prend et retourne une Map<Integer, String>
        private Map<String, Object> processRowWithoutMapping(Map<Integer, String> data) {
            Map<String, Object> rowData = new LinkedHashMap<>();
            
            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i);
                if (columnName == null) continue;

                String value = data.get(i);
                if (value != null && !value.trim().isEmpty()) {
                    rowData.put(columnName, value.trim());
                }
            }
            return rowData;
        }

        // MODIFIÉ : Renommé et simplifié pour travailler avec des String
        private Map<Integer, String> fillMergedCells(Map<Integer, String> data) {
            Map<Integer, String> currentRow = new HashMap<>();
            
            for (int i = 0; i < headers.size(); i++) {
                String cellValue = data.get(i);
                
                if (cellValue == null || cellValue.trim().isEmpty()) {
                    if (lastRowData.containsKey(i)) {
                        currentRow.put(i, lastRowData.get(i));
                    }
                } else {
                    currentRow.put(i, cellValue);
                }
            }
            
            currentRow.forEach(lastRowData::put);
            
            return currentRow;
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

    /**
     * MODIFICATION FINALE : Algorithme de détection de layout amélioré.
     * DESCRIPTION : L'algorithme est maintenant plus tolérant et précis.
     * - Il ignore les lignes entièrement vides.
     * - Il donne un bonus important si une ligne contient des en-têtes qui ne sont PAS
     * présents dans les lignes suivantes (les en-têtes sont uniques).
     * - Il pénalise moins lourdement la présence de quelques chiffres.
     */
     private LayoutAnalysis detectLayout(InputStream fileStream, int sheetIndex) {
        LayoutDetectionListener listener = new LayoutDetectionListener();
        try {
            EasyExcel.read(fileStream, listener).sheet(sheetIndex).headRowNumber(0).doRead();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                logger.debug("Layout detection read interrupted as expected after hitting row limit.");
            } else {
                throw new RuntimeException(e);
            }
        }
        
        List<Map<Integer, String>> headRows = listener.getRows();
        int bestScore = -100;
        int bestHeaderRowIndex = 1;

        for (int i = 0; i < headRows.size(); i++) {
            Map<Integer, String> row = headRows.get(i);
            if (row == null || row.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
                continue;
            }
            int score = 0;
            Collection<String> values = row.values().stream().filter(v -> v != null && !v.trim().isEmpty()).collect(Collectors.toList());
            long nonEmptyCells = values.size();
            if (nonEmptyCells < 2) {
                score -= 20;
            } else {
                long distinctValues = values.stream().distinct().count();
                if (distinctValues == nonEmptyCells) score += 10;
                long numericCount = values.stream().filter(v -> NUMERIC_PATTERN.matcher(v).matches()).count();
                if (numericCount <= nonEmptyCells / 3) score += 5;
                if (i + 3 < headRows.size()) {
                    Set<String> nextThreeRowsValues = new HashSet<>();
                    headRows.subList(i + 1, i + 4).forEach(r -> nextThreeRowsValues.addAll(r.values()));
                    if (Collections.disjoint(values, nextThreeRowsValues)) score += 15;
                }
            }
            logger.debug("Layout detection for sheet index {}: Row {} score: {}", sheetIndex, (i + 1), score);
            if (score > bestScore) {
                bestScore = score;
                bestHeaderRowIndex = i + 1;
            }
        }
        return new LayoutAnalysis(bestHeaderRowIndex, bestScore > 0);
    }

    /**
     * NOUVEAU : Implémentation de la logique de retraitement.
     */
    @Override
    @Transactional
    public void reprocessSheet(Long sheetId, int headerRowIndex) throws IOException {
        SheetEntity sheetEntity = sheetRepository.findById(sheetId)
            .orElseThrow(() -> new EntityNotFoundException("Sheet not found with ID: " + sheetId));
        FileEntity fileEntity = sheetEntity.getFile();

        // On vide les anciennes données de la feuille
        List<Long> rowIdsToDelete = sheetEntity.getRows().stream().map(RowEntity::getId).collect(Collectors.toList());
        if (!rowIdsToDelete.isEmpty()) {
            historyRepository.deleteByRowEntityIds(rowIdsToDelete);
            rowRepository.deleteAllByIdInBatch(rowIdsToDelete); // Plus performant pour les grosses suppressions
        }
        sheetEntity.setRows(new ArrayList<>());
        sheetEntity.setTotalRows(0);
        
        // On utilise la nouvelle méthode `getStoredFileStream` pour relire le fichier.
        try (InputStream headerStream = fileEntity.getStoredFileStream(fileStorageLocation);
             InputStream dataStream = fileEntity.getStoredFileStream(fileStorageLocation)) {
            
            List<String> headers = extractHeaders(headerStream, sheetEntity.getSheetIndex(), headerRowIndex);
            sheetEntity.setHeadersJson(objectMapper.writeValueAsString(headers));
            
            AtomicInteger processedRowsInSheet = new AtomicInteger(0);
            ExcelDataListener dataListener = new ExcelDataListener(sheetEntity, headers, processedRowsInSheet, new ArrayList<>(), maxRows, new AtomicInteger(0), headerRowIndex, Optional.empty());
            
            EasyExcel.read(dataStream, dataListener)
                .sheet(sheetEntity.getSheetIndex())
                .headRowNumber(0)
                .doRead();

            sheetEntity.setTotalRows(processedRowsInSheet.get());
            sheetRepository.save(sheetEntity);
        }
    }
}