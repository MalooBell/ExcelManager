package excel_upload_service.service.impl;

import excel_upload_service.dto.python.ColumnSchema;
import excel_upload_service.dto.python.SheetData;
import excel_upload_service.service.SchemaManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SchemaManagerServiceImpl implements SchemaManagerService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaManagerServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;

    public SchemaManagerServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String createTableFromSchema(Long fileId, SheetData sheetData) {
        // 1. Générer un nom de table sécurisé et unique
        String tableName = generateTableName(fileId, sheetData.getSheetName());

        // 2. Construire la requête CREATE TABLE
        // On commence par la définition d'une clé primaire
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (id BIGINT AUTO_INCREMENT PRIMARY KEY, ";

        // On ajoute chaque colonne en se basant sur le schéma reçu de Python
        String columnsSql = sheetData.getSchema().stream()
                .map(this::convertColumnSchemaToSql)
                .collect(Collectors.joining(", "));

        createTableSql += columnsSql + ");";

        logger.info("Exécution de la requête DDL : {}", createTableSql);

        // 3. Exécuter la requête
        jdbcTemplate.execute(createTableSql);

        return tableName;
    }

    /**
     * Génère un nom de table sûr en nettoyant les entrées.
     */
    private String generateTableName(Long fileId, String sheetName) {
        // Nettoie le nom de la feuille pour le rendre compatible avec SQL
        String cleanSheetName = sheetName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        return "dynamic_table_f" + fileId + "_" + cleanSheetName;
    }

    /**
     * Convertit notre DTO ColumnSchema en un fragment de SQL valide.
     * C'est une étape de sécurité cruciale pour éviter l'injection SQL.
     */
    private String convertColumnSchemaToSql(ColumnSchema column) {
        String columnName = "`" + column.getName().replaceAll("[^a-zA-Z0-9_]", "") + "`"; // Nettoyage et échappement du nom
        String columnType = switch (column.getType().toUpperCase()) {
            case "INTEGER" -> "BIGINT";
            case "DECIMAL(18, 4)" -> "DECIMAL(18, 4)";
            case "DATETIME" -> "DATETIME";
            default -> "VARCHAR(255)"; // Type par défaut sécurisé
        };
        return columnName + " " + columnType;
    }

    
}