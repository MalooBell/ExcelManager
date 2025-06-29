package excel_upload_service.dto.python;

import excel_upload_service.dto.python.SheetData;

public interface SchemaManagerService {
    /**
     * Assure qu'une table existe pour la feuille donnée et retourne son nom.
     * Si la table n'existe pas, elle est créée.
     *
     * @param fileId L'ID du fichier parent.
     * @param sheetData Les données de la feuille contenant le schéma.
     * @return Le nom de la table généré et validé.
     */
    String createTableFromSchema(Long fileId, SheetData sheetData);
}