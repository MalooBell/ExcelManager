// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/impl/ExcelUploadServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.dto.python.ColumnSchema; // On aura besoin de cette classe pour le mapping
import excel_upload_service.dto.python.ExcelProcessingResponse;
import excel_upload_service.dto.python.SheetData;
import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.service.ExcelUploadService;
import excel_upload_service.service.PythonProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);

    private final PythonProcessorService pythonProcessorService;
    private final FileEntityRepository fileRepository;
    private final SheetEntityRepository sheetRepository;
    private final RowEntityRepository rowRepository;
    private final ObjectMapper objectMapper;

    public ExcelUploadServiceImpl(PythonProcessorService pythonProcessorService,
                                  FileEntityRepository fileRepository,
                                  SheetEntityRepository sheetRepository,
                                  RowEntityRepository rowRepository,
                                  ObjectMapper objectMapper) {
        this.pythonProcessorService = pythonProcessorService;
        this.fileRepository = fileRepository;
        this.sheetRepository = sheetRepository;
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UploadResponse processAndSave(MultipartFile file) {
        logger.info("Délégation du traitement du fichier {} au service Python.", file.getOriginalFilename());

        try {
            ExcelProcessingResponse pythonResponse = pythonProcessorService.processFile(file);

            if (pythonResponse == null || pythonResponse.getSheets() == null || pythonResponse.getSheets().isEmpty()) {
                logger.warn("Le service Python n'a retourné aucune donnée traitable pour le fichier {}.", file.getOriginalFilename());
                return new UploadResponse(false, "Le fichier semble vide ou ne contient aucune donnée valide.", null, 0);
            }

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(pythonResponse.getFileName());
            fileRepository.save(fileEntity);

            int totalRowsProcessed = 0;

            for (SheetData sheetData : pythonResponse.getSheets()) {
                SheetEntity sheetEntity = new SheetEntity();
                sheetEntity.setFile(fileEntity);
                sheetEntity.setSheetName(sheetData.getSheetName());
                sheetEntity.setSheetIndex(pythonResponse.getSheets().indexOf(sheetData));
                sheetEntity.setTotalRows(sheetData.getTotalRows());

                // --- DEBUT DE LA CORRECTION ---

                // 1. Extraire la liste des noms de colonnes (List<String>) depuis la liste d'objets (List<ColumnSchema>)
                List<String> headerNames = sheetData.getSchema().stream()
                                                      .map(ColumnSchema::getName)
                                                      .collect(Collectors.toList());

                // 2. Sérialiser cette simple liste de chaînes de caractères en JSON.
                // Le JSON stocké sera maintenant de la forme : "['En-tête 1', 'En-tête 2']"
                String headersJson = objectMapper.writeValueAsString(headerNames);
                sheetEntity.setHeadersJson(headersJson);

                // --- FIN DE LA CORRECTION ---
                
                sheetRepository.save(sheetEntity);

                if (sheetData.getData() != null && !sheetData.getData().isEmpty()) {
                    List<RowEntity> rowsToSave = new ArrayList<>();
                    for (Map<String, Object> rowData : sheetData.getData()) {
                        RowEntity rowEntity = new RowEntity();
                        rowEntity.setSheet(sheetEntity);
                        // Cette partie était déjà correcte, on la laisse inchangée.
                        rowEntity.setDataJson(objectMapper.writeValueAsString(rowData));
                        rowsToSave.add(rowEntity);
                    }
                    rowRepository.saveAll(rowsToSave);
                    totalRowsProcessed += rowsToSave.size();
                }
            }

            logger.info("Fichier {} traité et sauvegardé avec succès. {} lignes insérées.", file.getOriginalFilename(), totalRowsProcessed);
            return new UploadResponse(true, "Fichier traité avec succès.", null, totalRowsProcessed);

        } catch (JsonProcessingException e) {
            logger.error("Erreur de sérialisation/désérialisation JSON lors du traitement.", e);
            throw new RuntimeException("Erreur de format de données interne.", e);
        } catch (Exception e) {
            logger.error("Une erreur est survenue lors de la sauvegarde des données traitées.", e);
            throw new RuntimeException("Erreur de sauvegarde en base de données : " + e.getMessage(), e);
        }
    }
}