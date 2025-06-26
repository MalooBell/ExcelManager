package excel_upload_service.controller;

import excel_upload_service.dto.UploadResponse;
import excel_upload_service.service.ExcelUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        // --- Validation de base ---
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(false, "Le fichier est vide.", null, 0));
        }

        // On peut conserver une validation de base sur le nom du fichier
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
             return ResponseEntity.badRequest()
                    .body(new UploadResponse(false, "Format de fichier non supporté. Seuls les fichiers Excel (.xlsx, .xls) sont acceptés.", null, 0));
        }

        // --- Délégation du traitement ---
        try {
            // On appelle la nouvelle méthode de notre service qui orchestre tout
            UploadResponse response = excelUploadService.processAndSave(file);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // En cas d'erreur (problème de communication avec Python, problème de BDD...), on retourne une erreur 500
            // Il est important de logger l'erreur côté serveur, ce que notre service fait déjà.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(false, "Une erreur inattendue est survenue: " + e.getMessage(), null, 0));
        }
    }

    
}