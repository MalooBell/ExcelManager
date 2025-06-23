// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/ModificationHistoryController.java
package excel_upload_service.controller;

import excel_upload_service.model.ModificationHistory;
import excel_upload_service.service.ModificationHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin
public class ModificationHistoryController {

    private final ModificationHistoryService historyService;

    public ModificationHistoryController(ModificationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/row/{rowId}")
    public List<ModificationHistory> getHistoryForRow(@PathVariable Long rowId) {
        return historyService.getHistoryForRow(rowId);
    }
    
    // NOUVEL ENDPOINT
    @GetMapping("/sheet/{sheetId}")
    public ResponseEntity<Page<ModificationHistory>> getHistoryForSheet(
            @PathVariable Long sheetId,
            Pageable pageable) {
        return ResponseEntity.ok(historyService.getHistoryForSheet(sheetId, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ModificationHistory>> getAllHistory() {
        List<ModificationHistory> allHistories = historyService.getAllHistories();
        return ResponseEntity.ok(allHistories);
    }
}