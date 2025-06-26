// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/ExcelPreviewServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.LayoutAnalysis;
import excel_upload_service.dto.SheetPreviewDto;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.service.ExcelPreviewService;
import excel_upload_service.utils.ExcelStructureAnalyzer; // Ensure this is imported
import excel_upload_service.utils.ExcelUtils; // Ensure this is imported (for extractHeaders if used here)
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelPreviewServiceImpl implements ExcelPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelPreviewServiceImpl.class);

    private final FileEntityRepository fileRepository;
    private final ObjectMapper objectMapper;
    private final ExcelStructureAnalyzer excelStructureAnalyzer; // Inject the analyzer

    @Value("${upload.dir}")
    private String uploadDir; // To access stored files

    public ExcelPreviewServiceImpl(FileEntityRepository fileRepository, ObjectMapper objectMapper, ExcelStructureAnalyzer excelStructureAnalyzer) {
        this.fileRepository = fileRepository;
        this.objectMapper = objectMapper;
        this.excelStructureAnalyzer = excelStructureAnalyzer; // Initialize the analyzer
    }

    @Override
    public SheetPreviewDto getSheetPreview(Long fileId, int sheetIndex, int rowLimit) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found with ID: " + fileId));

        Path filePath = Paths.get(uploadDir).resolve(fileEntity.getFileName());
        if (!Files.exists(filePath)) {
            throw new IOException("File not found on disk: " + filePath.toString());
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            SheetEntity sheetEntity = fileEntity.getSheets().stream()
                .filter(s -> s.getSheetIndex() == sheetIndex)
                .findFirst()
                .orElse(null);

            // Determine effective header row index for reading data based on stored header or default to 0
            int effectiveHeaderRowIndex = (sheetEntity != null && sheetEntity.getHeaderRowIndex() != -1)
                                        ? sheetEntity.getHeaderRowIndex()
                                        : 0; // Default to 0 if no header found or for general preview

            List<Map<String, String>> previewDataMaps;
            try (InputStream dataReadingStream = Files.newInputStream(filePath)) { // Fresh stream for data reading
                previewDataMaps = ExcelUtils.readExcelSheet(dataReadingStream, sheetIndex, effectiveHeaderRowIndex);
            }

            List<String> headers;
            if (sheetEntity != null && sheetEntity.getHeadersJson() != null && !sheetEntity.getHeadersJson().isEmpty()) {
                headers = objectMapper.readValue(sheetEntity.getHeadersJson(), List.class);
            } else {
                try (InputStream headerExtractionStream = Files.newInputStream(filePath)) {
                    headers = ExcelUtils.extractHeadersFromSpecificRow(headerExtractionStream, sheetIndex, effectiveHeaderRowIndex);
                }
            }

            // --- START MODIFICATION FOR SheetPreviewDto constructor ---
            // Convert List<Map<String, String>> to List<List<String>>
            List<List<String>> previewDataLists = previewDataMaps.stream()
                .limit(rowLimit)
                .map(rowMap -> {
                    List<String> rowAsList = new ArrayList<>();
                    // Ensure the order of values in the list matches the order of headers
                    // Fill with null if a header column is missing from the rowMap
                    for (String header : headers) {
                        rowAsList.add(rowMap.getOrDefault(header, null));
                    }
                    return rowAsList;
                })
                .collect(Collectors.toList());

            // --- MODIFIED LINE: Remove 'headers' from the constructor call ---
            return new SheetPreviewDto(previewDataLists); // <--- THIS IS THE ONLY CHANGE
            // --- END MODIFICATION ---
        }
    }

    @Override
    public LayoutAnalysis analyzeSheetLayout(InputStream inputStream, int sheetIndex) throws IOException {
        // This method directly calls the ExcelStructureAnalyzer
        return excelStructureAnalyzer.analyze(inputStream, sheetIndex);
    }
}