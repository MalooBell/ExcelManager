// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/RowEntityService.java
package excel_upload_service.service;

import excel_upload_service.dto.RowEntityDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RowEntityService {
    
    // La recherche se fait maintenant par ID de feuille
    Page<RowEntityDto> searchBySheetId(Long sheetId, String keyword, Pageable pageable);

    RowEntityDto getById(Long id);

    // La création se fait maintenant dans le contexte d'une feuille
    RowEntityDto create(Long sheetId, RowEntityDto dto);

    RowEntityDto update(Long id, RowEntityDto dto);

    void delete(Long id);
    
    // Cette méthode de recherche globale peut être conservée si vous avez un tableau de bord global
    Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable);
}