package excel_upload_service.controller;

import excel_upload_service.service.ResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reset")
@CrossOrigin
public class ResetController {

    private final ResetService resetService;

    public ResetController(ResetService resetService) {
        this.resetService = resetService;
    }

    @DeleteMapping
    public ResponseEntity<String> reset() {
        resetService.resetAll();
        return ResponseEntity.ok("Réinitialisation complète effectuée.");
    }
}
