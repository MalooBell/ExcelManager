// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/SheetMappingTemplateController.java
package excel_upload_service.controller;

import excel_upload_service.dto.SheetMappingTemplateDto;
import excel_upload_service.service.SheetMappingTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NOUVEAU : Contrôleur pour exposer les opérations CRUD sur les modèles de mapping.
 */
@RestController
@RequestMapping("/api/mappings/templates")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SheetMappingTemplateController {

    private final SheetMappingTemplateService templateService;

    public SheetMappingTemplateController(SheetMappingTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<SheetMappingTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SheetMappingTemplateDto> getTemplateById(@PathVariable Long id) {
        return templateService.getTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SheetMappingTemplateDto> createTemplate(@RequestBody SheetMappingTemplateDto templateDto) {
        SheetMappingTemplateDto created = templateService.createTemplate(templateDto);
        return ResponseEntity.status(201).body(created); // 201 Created
    }

    @PutMapping("/{id}")
    public ResponseEntity<SheetMappingTemplateDto> updateTemplate(@PathVariable Long id, @RequestBody SheetMappingTemplateDto templateDto) {
        return templateService.updateTemplate(id, templateDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}