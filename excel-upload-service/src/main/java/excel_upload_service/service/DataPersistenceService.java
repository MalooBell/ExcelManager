// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/DataPersistenceService.java
package excel_upload_service.service;

import excel_upload_service.dto.python.ExcelProcessingResponse;

public interface DataPersistenceService {
    /**
     * Sauvegarde les données traitées reçues d'un worker.
     * @param fileId L'ID de l'entité fichier parente.
     * @param processedData Les données structurées à sauvegarder.
     */
    void saveProcessedData(Long fileId, ExcelProcessingResponse processedData);
}