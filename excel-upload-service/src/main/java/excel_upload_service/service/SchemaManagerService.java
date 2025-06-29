// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/SchemaManagerService.java

package excel_upload_service.service;

import java.util.List;
import java.util.Map;

import excel_upload_service.dto.python.SheetData;

/**
 * Service pour gérer dynamiquement le schéma de la base de données.
 * Permet la création de tables et l'insertion de données de manière flexible.
 */
public interface SchemaManagerService {

    // /**
    //  * Crée une table dans la base de données basée sur un nom et une définition de colonnes.
    //  * La méthode doit être idempotente (ne pas échouer si la table existe déjà).
    //  *
    //  * @param tableName Le nom de la table à créer.
    //  * @param columns   Un Map où les clés sont les noms des colonnes et les valeurs leurs types de données SQL (ex: "VARCHAR(255)", "INT", "DOUBLE").
    //  */
    // void createTable(String tableName, Map<String, String> columns);

    // /**
    //  * Insère plusieurs lignes de données dans une table spécifiée.
    //  *
    //  * @param tableName Le nom de la table cible pour l'insertion.
    //  * @param data      Une liste de Map. Chaque Map représente une ligne, où les clés sont les noms des colonnes
    //  * et les valeurs sont les données à insérer.
    //  */
    // void insertData(String tableName, List<Map<String, Object>> data);

    String createTableFromSchema(Long fileId, SheetData sheetData) ;

}