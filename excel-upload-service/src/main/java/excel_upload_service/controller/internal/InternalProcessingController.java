// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/controller/internal/InternalProcessingController.java
package excel_upload_service.controller.internal;

import excel_upload_service.dto.python.ExcelProcessingResponse;
import excel_upload_service.service.DataPersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// On utilise un chemin distinct pour les API internes
@RequestMapping("/api/internal/files")
public class InternalProcessingController {

    private final DataPersistenceService dataPersistenceService;

    public InternalProcessingController(DataPersistenceService dataPersistenceService) {
        this.dataPersistenceService = dataPersistenceService;
    }

    /**
     * Point d'API appelé par le worker Python pour soumettre les données traitées.
     *
     * @param fileId L'ID du fichier correspondant dans la base de données.
     * @param processedData Les données extraites du fichier Excel.
     * @return Une réponse de succès ou d'échec.
     */
    @PostMapping("/{fileId}/processed-data")
    public ResponseEntity<String> receiveProcessedData(
            @PathVariable Long fileId,
            @RequestBody ExcelProcessingResponse processedData
    ) {
        try {
            dataPersistenceService.saveProcessedData(fileId, processedData);
            return ResponseEntity.ok("Données pour le fichier " + fileId + " sauvegardées avec succès.");
        } catch (Exception e) {
            // En cas d'erreur, on logue et on renvoie une erreur 500 au worker Python.
            // Cela pourrait déclencher une logique de nouvelle tentative ou d'alerte.
            return ResponseEntity.internalServerError().body("Erreur lors de la sauvegarde des données pour le fichier " + fileId + ": " + e.getMessage());
        }
    }
}