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
        try {
            // Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, "Le fichier est vide", null, 0));
            }

            String contentType = file.getContentType();
            if (!isExcelFile(contentType)) {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse(false, "Format de fichier non supporté. Seuls les fichiers Excel (.xlsx, .xls) sont acceptés.", null, 0));
            }

            // Traitement du fichier
            UploadResponse response = excelUploadService.uploadAndProcessExcel(file);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(false, "Erreur lors du traitement: " + e.getMessage(), null, 0));
        }
    }

    private boolean isExcelFile(String contentType) {
        return contentType != null && (
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                        contentType.equals("application/vnd.ms-excel") ||
                        contentType.equals("application/octet-stream")
        );
    }
}