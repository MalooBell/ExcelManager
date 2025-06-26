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
import excel_upload_service.service.ExcelPreviewService; // <--- ADD THIS IMPORT
import excel_upload_service.service.ExcelUploadService;
import excel_upload_service.utils.ExcelUtils;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
    private final ExcelPreviewService excelPreviewService; // <--- ADD THIS FIELD

    @Value("${upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    @Value("${excel.upload.max-file-size:10485760}")
    private long maxFileSize;
    @Value("${excel.upload.max-rows:1000700}")
    private int maxRows;

    public ExcelUploadServiceImpl(RowEntityRepository rowRepository, FileEntityRepository fileRepository,
                                  SheetEntityRepository sheetRepository, SheetMappingRepository sheetMappingRepository,
                                  ObjectMapper objectMapper, ModificationHistoryRepository historyRepository,
                                  ExcelPreviewService excelPreviewService) { // <--- ADD EXCELPREVIEWSERVICE TO CONSTRUCTOR
        this.rowRepository = rowRepository;
        this.fileRepository = fileRepository;
        this.sheetRepository = sheetRepository;
        this.sheetMappingRepository = sheetMappingRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
        this.excelPreviewService = excelPreviewService; // <--- INITIALIZE IT
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize(); // Use uploadDir
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage des fichiers.", ex);
        }
    }

    @PostConstruct
    public void init() {
        try {
            // Ici, 'uploadDir' n'est plus null
            this.uploadPath = Paths.get(uploadDir);
            if (Files.notExists(this.uploadPath)) {
                Files.createDirectories(this.uploadPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire de téléchargement : " + uploadDir, e);
        }
    }

    @Override
    @Transactional
    public UploadResponse uploadAndProcessExcel(MultipartFile file) {
        try {
            // Ensure upload directory exists and use it for fileStorageLocation initialization
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique file name to avoid collisions
            String uniqueFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING); // Add REPLACE_EXISTING

            logger.info("Starting initial upload for file: {}", file.getOriginalFilename());

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(uniqueFileName);
            fileEntity.setUploadTimestamp(LocalDateTime.now());
            fileEntity.setProcessed(false); // Initial state: not fully processed
            fileEntity.setNeedsHeaderValidation(false); // Initial state: no validation needed (will be updated)
            fileEntity.setTotalProcessedRows(0); // Initialize total processed rows
            fileEntity = fileRepository.save(fileEntity); // Save to get the ID

            final Long fileId = fileEntity.getId();
            List<Integer> sheetsNeedingValidation = new ArrayList<>();

            // Use the stored file for processing to ensure consistency
            try (InputStream storedFileInputStream = Files.newInputStream(filePath)) { // Use the saved file's input stream
                List<com.alibaba.excel.read.metadata.ReadSheet> sheetMetadatas = EasyExcel.read(storedFileInputStream).build().excelExecutor().sheetList();

                if (sheetMetadatas.isEmpty()) {
                    // Delete the file entity and the physical file if no sheets are found
                    fileRepository.delete(fileEntity);
                    Files.deleteIfExists(filePath); // Delete physical file
                    logger.warn("The uploaded file {} does not contain any sheets or is not a valid Excel file.", file.getOriginalFilename());
                    return new UploadResponse(false, "The file does not contain any sheets or is not a valid Excel file.", null);
                }

                for (com.alibaba.excel.read.metadata.ReadSheet sheetMetadata : sheetMetadatas) {
                try (InputStream analysisInputStream = Files.newInputStream(filePath)) {
                    LayoutAnalysis layoutAnalysis = excelPreviewService.analyzeSheetLayout(analysisInputStream, sheetMetadata.getSheetNo());

                    // --- START MODIFICATION FOR EFFECTIVELY FINAL sheetEntity ---
                    // Declare and initialize sheetEntity within the loop,
                    // and use a separate variable for the initial creation if needed before saving.
                    SheetEntity sheetEntityForProcessing = new SheetEntity(); // New variable for this iteration
                    sheetEntityForProcessing.setFile(fileEntity);
                    sheetEntityForProcessing.setSheetName(sheetMetadata.getSheetName());
                    sheetEntityForProcessing.setSheetIndex(sheetMetadata.getSheetNo());
                    sheetEntityForProcessing.setTotalRows(0);

                    List<String> headers = new ArrayList<>(); // Initialize headers list

                    if (layoutAnalysis.getHeaderRowIndex() == -1) {
                        logger.warn("Sheet '{}' (index {}) has no valid headers based on detected layout, marking for manual validation.",
                                sheetMetadata.getSheetName(), sheetMetadata.getSheetNo());
                        sheetEntityForProcessing.setHeaderRowIndex(-1);
                        fileEntity.setNeedsHeaderValidation(true);
                        sheetsNeedingValidation.add(sheetMetadata.getSheetNo());
                    } else {
                        sheetEntityForProcessing.setHeaderRowIndex(layoutAnalysis.getHeaderRowIndex());
                        try (InputStream headerExtractionStream = Files.newInputStream(filePath)) {
                            headers = extractHeaders(headerExtractionStream, sheetMetadata.getSheetNo(), layoutAnalysis.getHeaderRowIndex());
                            sheetEntityForProcessing.setHeadersJson(objectMapper.writeValueAsString(headers));
                        }
                    }

                    // Save the sheetEntityForProcessing, which returns the managed entity.
                    // This new reference will be effectively final for subsequent usage in this iteration.
                    final SheetEntity savedSheetEntity = sheetRepository.save(sheetEntityForProcessing); // <--- KEY CHANGE: use final and new variable

                    logger.debug("Saved SheetEntity for sheet '{}' (ID: {})", sheetMetadata.getSheetName(), savedSheetEntity.getId());

                    // ONLY process rows if a header was found (i.e., headerRowIndex is not -1)
                    if (savedSheetEntity.getHeaderRowIndex() != -1) { // Use savedSheetEntity
                        try (InputStream dataInputStream = Files.newInputStream(filePath)) {
                            // Pass the effectively final 'savedSheetEntity' to the listener
                            ExcelDataListener dataListener = new ExcelDataListener(savedSheetEntity, headers, new AtomicInteger(0), new ArrayList<>(), maxRows, new AtomicInteger(0), savedSheetEntity.getHeaderRowIndex(), Optional.empty()); // <--- Use savedSheetEntity
                            
                            List<Map<String, String>> sheetData = ExcelUtils.readExcelSheet(dataInputStream, sheetMetadata.getSheetNo(), savedSheetEntity.getHeaderRowIndex()); // <--- Use savedSheetEntity's header index
                            savedSheetEntity.setTotalRows(sheetData.size()); // <--- Use savedSheetEntity

                            List<RowEntity> rowEntities = sheetData.stream().map(rowData -> {
                                RowEntity rowEntity = new RowEntity();
                                rowEntity.setSheet(savedSheetEntity); // <--- Use savedSheetEntity
                                try {
                                    rowEntity.setDataJson(objectMapper.writeValueAsString(rowData));
                                } catch (Exception e) {
                                    logger.error("Error converting row data to JSON for sheet {}: {}", savedSheetEntity.getSheetName(), e.getMessage());
                                    rowEntity.setDataJson("{}");
                                }
                                return rowEntity;
                            }).collect(Collectors.toList());
                            rowRepository.saveAll(rowEntities);
                            logger.info("Processed sheet '{}' with {} rows. Header row: {}",
                                    savedSheetEntity.getSheetName(), sheetData.size(), savedSheetEntity.getHeaderRowIndex());
                        }
                    } else {
                        logger.info("Skipping row processing for sheet '{}' (index {}) as no valid header was found automatically.",
                                sheetMetadata.getSheetName(), sheetMetadata.getSheetNo());
                    }
                    } // End try-with-resources for analysisInputStream
                }
            } // End try-with-resources for storedFileInputStream

            // After processing all sheets, update the file entity's status
            if (!sheetsNeedingValidation.isEmpty()) {
                fileEntity.setNeedsHeaderValidation(true);
                fileEntity.setProcessed(false); // Not fully processed until manual validation is complete
            } else {
                fileEntity.setProcessed(true); // Mark as fully processed if no validation needed
                fileEntity.setNeedsHeaderValidation(false); // Explicitly false
            }
            fileRepository.save(fileEntity); // Save updated file entity (including needsHeaderValidation and processed)

            // Return appropriate response
            if (fileEntity.isNeedsHeaderValidation()) {
                logger.info("File {} requires manual header validation. Redirecting to validation page.", file.getOriginalFilename());
                return new UploadResponse(true, "File requires manual header validation. Please complete on validation page.", fileEntity.getId());
            } else {
                logger.info("File {} uploaded and processed successfully.", file.getOriginalFilename());
                return new UploadResponse(true, "File uploaded and processed successfully!", fileEntity.getId());
            }

        } catch (Exception e) {
            logger.error("Failed to upload and process Excel file: {}", e.getMessage(), e);
            // Ensure the file entity is deleted if upload fails at any point after initial save
            // This needs to be handled carefully if fileEntity is already persisted and an error occurs midway.
            // For now, let's just return the error response.
            return new UploadResponse(false, "Failed to upload and process Excel file: " + e.getMessage(), null);
        }
    }
    
    /**
     * NOUVEAU : Méthode qui contient l'ancienne logique de traitement de toutes les feuilles.
     * Elle sera appelée si la détection automatique est un succès.
     * NOTE: Cette méthode semble redondante now that processSheet is integrated into uploadAndProcessExcel.
     * It might be removed or refactored later if its logic is fully absorbed.
     * For now, keeping it but it's not currently called in the main upload flow.
     */
    private void processAllSheets(MultipartFile file, FileEntity fileEntity) throws IOException {
         try (InputStream is = file.getInputStream()) {
                List<com.alibaba.excel.read.metadata.ReadSheet> sheetMetadatas = EasyExcel.read(is).build().excelExecutor().sheetList();
                for (com.alibaba.excel.read.metadata.ReadSheet sheetInfo : sheetMetadatas) {
                     try(InputStream sheetStream = file.getInputStream()){ // Re-open stream for each sheet
                         LayoutAnalysis layout = detectLayout(sheetStream, sheetInfo.getSheetNo());
                         // This part needs adjustment if processSheet is to be reused here.
                         // For now, it's not actively used in the main upload flow.
                         // try(InputStream processStream = file.getInputStream()){
                         //     processSheet(processStream, sheetInfo, layout, fileEntity, new AtomicInteger(0), new ArrayList<>());
                         // }
                     }
                }
            }
    }
    
    // Removed processSheet method as its core logic is now integrated directly into uploadAndProcessExcel for better flow control.
    // private void processSheet(...) {...}

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
                logger.debug("Header extraction read interrupted as expected.");
            } else {
                logger.error("Error during header extraction for sheet index {}: {}", sheetIndex, e.getMessage(), e);
                throw e;
            }
        }
        return headerListener.getHeaders();
    }

    // private LayoutAnalysis detectLayout(InputStream fileStream, int sheetIndex) { // Renamed from public to private
    //     LayoutDetectionListener listener = new LayoutDetectionListener();
    //     try {
    //         EasyExcel.read(fileStream, listener).sheet(sheetIndex).headRowNumber(0).doRead();
    //     } catch (Exception e) {
    //         if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
    //             logger.debug("Layout detection read interrupted as expected after hitting row limit.");
    //         } else {
    //             logger.error("Error during layout detection for sheet index {}: {}", sheetIndex, e.getMessage(), e);
    //             throw new RuntimeException(e);
    //         }
    //     }
        
    //     List<Map<Integer, String>> headRows = listener.getRows();
    //     int bestScore = -100;
    //     int bestHeaderRowIndex = -1; // Initialize to -1 to indicate no header found

    //     for (int i = 0; i < headRows.size(); i++) {
    //         Map<Integer, String> row = headRows.get(i);
    //         if (row == null || row.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
    //             continue; // Skip entirely empty rows
    //         }
    //         int score = 0;
    //         Collection<String> values = row.values().stream().filter(v -> v != null && !v.trim().isEmpty()).collect(Collectors.toList());
    //         long nonEmptyCells = values.size();
            
    //         if (nonEmptyCells < 2) {
    //             // Not enough cells to be a valid header
    //             score -= 20;
    //         } else {
    //             long distinctValues = values.stream().distinct().count();
    //             // Bonus for unique headers
    //             if (distinctValues == nonEmptyCells) {
    //                 score += 10;
    //             }
                
    //             long numericCount = values.stream().filter(v -> v != null && NUMERIC_PATTERN.matcher(v).matches()).count();
    //             // Penalize rows with too many numbers, but allow some
    //             if (numericCount > nonEmptyCells / 2) { // More than half are numbers, less likely to be header
    //                 score -= 15;
    //             } else if (numericCount <= nonEmptyCells / 3) { // Few numbers, good sign
    //                 score += 5;
    //             }

    //             // Look for disjoinment with subsequent rows (headers shouldn't repeat immediately as data)
    //             if (i + 3 < headRows.size()) {
    //                 Set<String> nextThreeRowsValues = new HashSet<>();
    //                 // Collect values from the next 3 relevant rows for comparison
    //                 for (int j = i + 1; j <= Math.min(i + 3, headRows.size() -1); j++) {
    //                     headRows.get(j).values().stream()
    //                         .filter(v -> v != null && !v.trim().isEmpty())
    //                         .forEach(nextThreeRowsValues::add);
    //                 }
    //                 if (Collections.disjoint(values, nextThreeRowsValues) && !nextThreeRowsValues.isEmpty()) {
    //                     score += 15; // Strong indicator if current row headers are not in next data rows
    //                 }
    //             }
    //         }
    //         logger.debug("Layout detection for sheet index {}: Row {} score: {}", sheetIndex, (i + 1), score);
    //         if (score > bestScore) {
    //             bestScore = score;
    //             bestHeaderRowIndex = i + 1; // Row index is 1-based in Excel
    //         }
    //     }
    //     // If no reasonable header was detected (score too low), set index to -1
    //     if (bestScore <= 0 && bestHeaderRowIndex != -1) { // Only reset if a candidate was found but score is poor
    //         return new LayoutAnalysis(-1, false);
    //     }
    //     return new LayoutAnalysis(bestHeaderRowIndex, bestHeaderRowIndex != -1);
    // }


    private static class HeaderListener implements ReadListener<Map<Integer, String>> {
        private final List<String> headers = new ArrayList<>();
        public List<String> getHeaders() { return headers; }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (headMap != null && !headMap.isEmpty()) {
                // Ensure headers are ordered by column index
                List<Integer> sortedKeys = new ArrayList<>(headMap.keySet());
                Collections.sort(sortedKeys);
                
                // Populate headers list based on sorted column indices
                // Fill nulls for skipped column indices to maintain correct mapping later
                int currentColumn = 0;
                for (Integer key : sortedKeys) {
                    // Fill with nulls for any columns skipped between the last header and current
                    while (currentColumn < key) {
                        headers.add(null);
                        currentColumn++;
                    }
                    ReadCellData<?> cell = headMap.get(key);
                    String cellValue = (cell != null) ? cell.getStringValue() : null;
                    headers.add(cellValue != null ? cellValue.trim() : null);
                    currentColumn++;
                }

                // Trim trailing nulls (if any, though usually not needed for headers)
                while (headers.size() > 0 && headers.get(headers.size() - 1) == null) {
                    headers.remove(headers.size() - 1);
                }
            }
            context.interrupt(); // Stop reading after header row
        }
        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {} // Not used for headers
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
        private final Map<Integer, String> lastRowData = new HashMap<>(); // Store last non-empty value for merged cells

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

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            int currentRowIndex = context.readRowHolder().getRowIndex() + 1; // EasyExcel row index is 0-based
            if (currentRowIndex <= this.headerRowIndex) { // Skip rows before and including the header
                return;
            }

            try {
                if (totalProcessedRowsInFile.get() >= maxRows) {
                     context.interrupt(); // Stop reading if global row limit is exceeded
                     if(errors.stream().noneMatch(e -> e.contains("Row limit exceeded"))) {
                        errors.add("Row limit exceeded: " + maxRows + " for file.");
                     }
                     return;
                }

                // Apply merged cell logic before processing data
                Map<Integer, String> filledData = fillMergedCells(data);

                Map<String, Object> rowData;
                if (hasMapping) {
                    rowData = processRowWithMapping(filledData);
                } else {
                    rowData = processRowWithoutMapping(filledData);
                }

                // Do not save entirely empty rows (all values are null or empty strings)
                if (rowData.isEmpty() || rowData.values().stream().allMatch(v -> v == null || (v instanceof String && ((String) v).trim().isEmpty()))) {
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
                logger.error("Error on sheet '{}' row {}: {}", sheetEntity.getSheetName(), rowIndex + 1, e.getMessage(), e);
                errors.add("Sheet " + sheetEntity.getSheetName() + " Row " + (rowIndex + 1) + ": " + e.getMessage());
            }
        }
        
        private Map<String, Object> processRowWithMapping(Map<Integer, String> data) {
            Map<String, Object> rowData = new LinkedHashMap<>();
            boolean ignoreUnmapped = mappingDto.map(SheetMappingDto::isIgnoreUnmapped).orElse(false);

            // Iterate through the extracted headers to match data by column index
            for (int i = 0; i < headers.size(); i++) {
                String sourceHeader = headers.get(i);
                if (sourceHeader == null || sourceHeader.trim().isEmpty()) {
                    continue; // Skip null or empty header names
                }

                String value = data.get(i); // Get value by column index
                // Trim value, but keep null if it's truly empty after trim
                String trimmedValue = (value != null) ? value.trim() : null;

                String destinationHeader = sourceToDestinationMap.get(sourceHeader);

                if (destinationHeader != null) {
                    if (trimmedValue != null && !trimmedValue.isEmpty()) {
                         rowData.put(destinationHeader, trimmedValue);
                    }
                } else if (!ignoreUnmapped) {
                    if (trimmedValue != null && !trimmedValue.isEmpty()) {
                        rowData.put(sourceHeader, trimmedValue);
                    }
                }
            }
            return rowData;
        }

        private Map<String, Object> processRowWithoutMapping(Map<Integer, String> data) {
            Map<String, Object> rowData = new LinkedHashMap<>();
            
            // Iterate through the extracted headers to map data correctly
            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i);
                if (columnName == null || columnName.trim().isEmpty()) {
                    continue; // Skip null or empty column names
                }

                String value = data.get(i); // Get value by column index
                String trimmedValue = (value != null) ? value.trim() : null;

                if (trimmedValue != null && !trimmedValue.isEmpty()) {
                    rowData.put(columnName, trimmedValue);
                }
            }
            return rowData;
        }

        // Fills in null or empty cells from the previous non-empty value (for merged cells effect)
        private Map<Integer, String> fillMergedCells(Map<Integer, String> data) {
            Map<Integer, String> currentRow = new HashMap<>();
            
            // Copy data to currentRow first
            currentRow.putAll(data);

            // Iterate over expected column indices based on headers size
            for (int i = 0; i < headers.size(); i++) {
                String cellValue = currentRow.get(i);
                
                // If current cell is empty or null, try to fill from lastRowData
                if (cellValue == null || cellValue.trim().isEmpty()) {
                    if (lastRowData.containsKey(i)) {
                        currentRow.put(i, lastRowData.get(i));
                    }
                } else {
                    // If current cell has a value, update lastRowData for this column
                    lastRowData.put(i, cellValue);
                }
            }
            // After processing the current row, update lastRowData with its values
            // This ensures that the next row can inherit from this one if its cells are empty.
            // This line is controversial in merge cell handling. The above loop already updates `lastRowData`
            // so `currentRow.forEach(lastRowData::put)` might be redundant or incorrect for some merge scenarios.
            // Keeping it for now as per original logic, but it might need review for complex merged cells.
            // lastRowData.putAll(currentRow); // Removed this, as current row already updates lastRowData for non-empty cells

            return currentRow;
        }


        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (!batchData.isEmpty()) {
                saveBatch();
            }
            logger.info("Sheet '{}' processed: {} rows. Total processed rows in file: {}", sheetEntity.getSheetName(), processedRowsInSheet.get(), totalProcessedRowsInFile.get());
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
                logger.error("Error during layout detection for sheet index {}: {}", sheetIndex, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        
        List<Map<Integer, String>> headRows = listener.getRows();
        int bestScore = -100;
        int bestHeaderRowIndex = -1; // Initialize to -1 to indicate no header found

        for (int i = 0; i < headRows.size(); i++) {
            Map<Integer, String> row = headRows.get(i);
            if (row == null || row.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
                continue; // Skip entirely empty rows
            }
            int score = 0;
            Collection<String> values = row.values().stream().filter(v -> v != null && !v.trim().isEmpty()).collect(Collectors.toList());
            long nonEmptyCells = values.size();
            
            if (nonEmptyCells < 2) {
                // Not enough cells to be a valid header
                score -= 20;
            } else {
                long distinctValues = values.stream().distinct().count();
                // Bonus for unique headers
                if (distinctValues == nonEmptyCells) {
                    score += 10;
                }
                
                long numericCount = values.stream().filter(v -> v != null && NUMERIC_PATTERN.matcher(v).matches()).count();
                // Penalize rows with too many numbers, but allow some
                if (numericCount > nonEmptyCells / 2) { // More than half are numbers, less likely to be header
                    score -= 15;
                } else if (numericCount <= nonEmptyCells / 3) { // Few numbers, good sign
                    score += 5;
                }

                // Look for disjoinment with subsequent rows (headers shouldn't repeat immediately as data)
                if (i + 3 < headRows.size()) {
                    Set<String> nextThreeRowsValues = new HashSet<>();
                    // Collect values from the next 3 relevant rows for comparison
                    for (int j = i + 1; j <= Math.min(i + 3, headRows.size() -1); j++) {
                        headRows.get(j).values().stream()
                            .filter(v -> v != null && !v.trim().isEmpty())
                            .forEach(nextThreeRowsValues::add);
                    }
                    if (Collections.disjoint(values, nextThreeRowsValues) && !nextThreeRowsValues.isEmpty()) {
                        score += 15; // Strong indicator if current row headers are not in next data rows
                    }
                }
            }
            logger.debug("Layout detection for sheet index {}: Row {} score: {}", sheetIndex, (i + 1), score);
            if (score > bestScore) {
                bestScore = score;
                bestHeaderRowIndex = i + 1; // Row index is 1-based in Excel
            }
        }
        // If no reasonable header was detected (score too low), set index to -1
        if (bestScore <= 0 && bestHeaderRowIndex != -1) { // Only reset if a candidate was found but score is poor
            return new LayoutAnalysis(-1, false);
        }
        return new LayoutAnalysis(bestHeaderRowIndex, bestHeaderRowIndex != -1);
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

        // On vide les anciennes données de la feuille et l'historique associé
        List<Long> rowIdsToDelete = sheetEntity.getRows().stream().map(RowEntity::getId).collect(Collectors.toList());
        if (!rowIdsToDelete.isEmpty()) {
            historyRepository.deleteByRowEntityIds(rowIdsToDelete); // Assuming this method exists
            rowRepository.deleteAllByIdInBatch(rowIdsToDelete); // More performant for large deletions
        }
        sheetEntity.setRows(new ArrayList<>()); // Clear old rows from entity in memory
        sheetEntity.setTotalRows(0);
        
        // Use the new getStoredFileStream method from FileEntity to get an InputStream
        try (InputStream headerStream = fileEntity.getStoredFileStream(fileStorageLocation);
             InputStream dataStream = fileEntity.getStoredFileStream(fileStorageLocation)) {
            
            // Extract headers based on the manually selected headerRowIndex
            List<String> headers = extractHeaders(headerStream, sheetEntity.getSheetIndex(), headerRowIndex);
            sheetEntity.setHeadersJson(objectMapper.writeValueAsString(headers));
            sheetEntity.setHeaderRowIndex(headerRowIndex); // Set the manually chosen header index
            
            AtomicInteger processedRowsInSheet = new AtomicInteger(0);
            // Pass Optional.empty() for sheet mapping as reprocessing doesn't directly involve existing mappings
            ExcelDataListener dataListener = new ExcelDataListener(sheetEntity, headers, processedRowsInSheet, new ArrayList<>(), maxRows, new AtomicInteger(0), headerRowIndex, Optional.empty());
            
            EasyExcel.read(dataStream, dataListener)
                .sheet(sheetEntity.getSheetIndex())
                .headRowNumber(0) // EasyExcel always starts reading from row 0; filtering is done in listener
                .doRead();

            sheetEntity.setTotalRows(processedRowsInSheet.get());
            sheetRepository.save(sheetEntity);

            // After reprocessing, update the file's status
            // Check if all sheets for this file are now validated
            boolean allSheetsValidated = sheetRepository.findByFile(fileEntity).stream()
                .allMatch(s -> s.getHeaderRowIndex() != -1 && s.getHeadersJson() != null && !s.getHeadersJson().isEmpty());
            
            if (allSheetsValidated) {
                fileEntity.setNeedsHeaderValidation(false);
                fileEntity.setProcessed(true); // Mark file as fully processed
            }
            fileRepository.save(fileEntity); // Save updated file entity
        }
    }
}