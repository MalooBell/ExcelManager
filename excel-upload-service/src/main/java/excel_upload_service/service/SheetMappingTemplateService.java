// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/SheetMappingTemplateService.java
package excel_upload_service.service;

import excel_upload_service.dto.SheetMappingTemplateDto;
import java.util.List;
import java.util.Optional;

/**
 * NOUVEAU : Interface pour le service gérant la logique métier des modèles de mapping.
 */
public interface SheetMappingTemplateService {

    List<SheetMappingTemplateDto> getAllTemplates();

    Optional<SheetMappingTemplateDto> getTemplateById(Long id);

    SheetMappingTemplateDto createTemplate(SheetMappingTemplateDto templateDto);

    Optional<SheetMappingTemplateDto> updateTemplate(Long id, SheetMappingTemplateDto templateDto);

    void deleteTemplate(Long id);
}