// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/ExcelUploadService.java
package excel_upload_service.service;

import excel_upload_service.dto.UploadResponse;
import excel_upload_service.dto.python.ExcelProcessingResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelUploadService {
    /**
     * Orchestre le traitement d'un fichier Excel en appelant un service externe (Python)
     * puis en sauvegardant les données structurées retournées.
     *
     * @param file Le fichier Excel original envoyé par l'utilisateur.
     * @return Une réponse résumant le succès ou l'échec de l'opération.
     */
    UploadResponse processAndSave(MultipartFile file);
}