// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/SheetMappingService.java
package excel_upload_service.service;

import excel_upload_service.dto.SheetMappingDto;
import java.util.Optional;

public interface SheetMappingService {

    Optional<SheetMappingDto> getMappingBySheetId(Long sheetId);

    SheetMappingDto createOrUpdateMapping(Long sheetId, SheetMappingDto mappingDto);

    // NOUVEAU : Méthode pour appliquer un modèle de mapping à une feuille.
    SheetMappingDto applyTemplateToSheet(Long sheetId, Long templateId);
}