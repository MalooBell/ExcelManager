// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/impl/DataPersistenceServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.python.ColumnSchema;
import excel_upload_service.dto.python.ExcelProcessingResponse;
import excel_upload_service.dto.python.SheetData;
import excel_upload_service.model.FileEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.service.DataPersistenceService;
import excel_upload_service.service.SchemaManagerService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(DataPersistenceServiceImpl.class);

    private final FileEntityRepository fileRepository;
    private final SchemaManagerService schemaManagerService; // NOTRE NOUVEL ARCHITECTE
    private final JdbcTemplate jdbcTemplate; // POUR LES INSERTIONS DYNAMIQUES
    private final ObjectMapper objectMapper;

    // Mise à jour du constructeur pour injecter les nouveaux services
    public DataPersistenceServiceImpl(FileEntityRepository fileRepository,
                                      SchemaManagerService schemaManagerService,
                                      JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper) {
        this.fileRepository = fileRepository;
        this.schemaManagerService = schemaManagerService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void saveProcessedData(Long fileId, ExcelProcessingResponse processedData) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("Fichier avec ID " + fileId + " non trouvé."));

        logger.info("Début de la sauvegarde dynamique pour le fichier : {}", fileEntity.getFileName());

        try {
            for (SheetData sheetData : processedData.getSheets()) {
                // 1. Créer la table dynamiquement à partir du schéma
                String tableName = schemaManagerService.createTableFromSchema(fileId, sheetData);

                // 2. Préparer et exécuter l'insertion des données en batch
                if (sheetData.getData() != null && !sheetData.getData().isEmpty()) {
                    insertDataIntoDynamicTable(tableName, sheetData.getSchema(), sheetData.getData());
                }
            }
            logger.info("Sauvegarde dynamique pour le fichier {} terminée.", fileEntity.getFileName());

        } catch (Exception e) {
            logger.error("Erreur critique lors de la sauvegarde dynamique pour le fichier {}.", fileId, e);
            // On pourrait vouloir mettre le statut du fichier en "ERREUR" ici
            throw new RuntimeException("Erreur lors de la sauvegarde dynamique.", e);
        }
    }

    /**
     * Insère les données dans la table dynamique nouvellement créée.
     */
    private void insertDataIntoDynamicTable(String tableName, List<ColumnSchema> schema, List<Map<String, Object>> data) {
        // Construit la requête INSERT : INSERT INTO `table_name` (`col1`, `col2`) VALUES (?, ?)
        String columns = schema.stream()
                .map(col -> "`" + col.getName().replaceAll("[^a-zA-Z0-9_]", "") + "`")
                .collect(Collectors.joining(", "));

        String placeholders = schema.stream()
                .map(col -> "?")
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        // Prépare les données pour une insertion en batch (plus performant)
        List<Object[]> batchArgs = data.stream()
                .map(row -> schema.stream()
                        .map(col -> row.get(col.getName()))
                        .toArray())
                .collect(Collectors.toList());

        logger.info("Insertion de {} lignes dans la table {}.", batchArgs.size(), tableName);

        // Exécute l'insertion en batch
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}