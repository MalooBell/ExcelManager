// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/SheetMappingController.java
package excel_upload_service.controller;

import excel_upload_service.dto.SheetMappingDto;
import excel_upload_service.service.SheetMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * NOUVEAU : Contrôleur pour gérer les opérations CRUD sur les mappings de feuille.
 */
@RestController
@RequestMapping("/api/sheets/{sheetId}/mapping") // L'URL de base est liée à une feuille spécifique
@CrossOrigin(origins = "*", maxAge = 3600)
public class SheetMappingController {

    private final SheetMappingService sheetMappingService;

    public SheetMappingController(SheetMappingService sheetMappingService) {
        this.sheetMappingService = sheetMappingService;
    }

    /**
     * Endpoint pour récupérer le mapping d'une feuille.
     * @param sheetId L'ID de la feuille (extrait de l'URL).
     * @return Un ResponseEntity contenant le DTO du mapping ou 404 Not Found.
     */
    @GetMapping
    public ResponseEntity<SheetMappingDto> getMapping(@PathVariable Long sheetId) {
        return sheetMappingService.getMappingBySheetId(sheetId)
                .map(ResponseEntity::ok) // Si le mapping existe, retourne 200 OK avec le DTO
                .orElse(ResponseEntity.notFound().build()); // Sinon, retourne 404
    }

    /**
     * Endpoint pour créer ou mettre à jour un mapping.
     * Il gère à la fois POST (création) et PUT (mise à jour) pour plus de simplicité.
     * @param sheetId L'ID de la feuille.
     * @param mappingDto Le corps de la requête contenant la configuration du mapping.
     * @return Le DTO du mapping sauvegardé avec un statut 200 OK.
     */
    @PostMapping
    public ResponseEntity<SheetMappingDto> createOrUpdateMapping(@PathVariable Long sheetId, @RequestBody SheetMappingDto mappingDto) {
        SheetMappingDto savedMapping = sheetMappingService.createOrUpdateMapping(sheetId, mappingDto);
        return ResponseEntity.ok(savedMapping);
    }

     /**
     * NOUVEAU : Endpoint pour appliquer un modèle à la feuille courante.
     * @param sheetId L'ID de la feuille cible.
     * @param templateId L'ID du modèle à appliquer.
     * @return Le DTO du mapping mis à jour.
     */
    @PostMapping("/apply-template/{templateId}")
    public ResponseEntity<SheetMappingDto> applyTemplate(@PathVariable Long sheetId, @PathVariable Long templateId) {
        SheetMappingDto updatedMapping = sheetMappingService.applyTemplateToSheet(sheetId, templateId);
        return ResponseEntity.ok(updatedMapping);
    }
}