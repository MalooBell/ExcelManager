// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/ExcelUploadController.java
package excel_upload_service.controller;

import excel_upload_service.dto.UploadResponse;
import excel_upload_service.service.ExcelUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections; // NOUVEAU : pour créer une liste d'erreurs simple.

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;

    public ExcelUploadController(ExcelUploadService excelUploadService) {
        this.excelUploadService = excelUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            // Validation du fichier
            if (file.isEmpty()) {
                // CORRIGÉ : Utilisation du nouveau constructeur pour les erreurs.
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, "Le fichier est vide"/*, Collections.singletonList("Le fichier ne peut pas être vide.")*/));
            }

            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            // Amélioration de la validation pour inclure les extensions de fichier
            if (!isExcelFile(contentType, fileName)) {
                 // CORRIGÉ : Utilisation du nouveau constructeur pour les erreurs.
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, "Format de fichier non supporté."/* , Collections.singletonList("Seuls les fichiers Excel (.xlsx, .xls) sont acceptés.")*/));
            }

            // Traitement du fichier par le service
            // Le service retourne déjà un UploadResponse correctement formaté.
            UploadResponse response = excelUploadService.uploadAndProcessExcel(file);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // CORRIGÉ : Utilisation du nouveau constructeur pour les erreurs.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(false, "Erreur interne lors du traitement: " + e.getMessage()/* , Collections.singletonList(e.getMessage())*/));
        }
    }
    
    /**
     * Méthode de validation améliorée qui vérifie le type MIME et l'extension du fichier.
     */
    private boolean isExcelFile(String contentType, String fileName) {
        // Validation par type MIME
        if (contentType != null && (
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("application/vnd.ms-excel")
        )) {
            return true;
        }
        // Validation de secours par extension de fichier
        if (fileName != null && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
            return true;
        }
        
        // Cas particulier pour les "octet-stream" qui peuvent être des fichiers Excel
        if("application/octet-stream".equals(contentType) && (fileName != null && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls")))){
            return true;
        }

        return false;
    }
}