package excel_upload_service.controller;

import excel_upload_service.dto.SheetPreviewDto;
import excel_upload_service.service.ExcelPreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/preview")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExcelPreviewController {

    private final ExcelPreviewService excelPreviewService;

    public ExcelPreviewController(ExcelPreviewService excelPreviewService) {
        this.excelPreviewService = excelPreviewService;
    }

    @GetMapping("/file/{fileId}/sheet/{sheetIndex}")
    public ResponseEntity<SheetPreviewDto> getSheetPreview(
            @PathVariable Long fileId,
            @PathVariable int sheetIndex,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            SheetPreviewDto preview = excelPreviewService.getSheetPreview(fileId, sheetIndex, limit);
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            // Gérer les erreurs de lecture de fichier
            return ResponseEntity.status(500).build();
        }
    }

    
}
