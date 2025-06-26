// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/repository/SheetMappingTemplateRepository.java
package excel_upload_service.repository;

import excel_upload_service.model.SheetMappingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * NOUVEAU : Repository pour l'entité SheetMappingTemplate.
 */
@Repository
public interface SheetMappingTemplateRepository extends JpaRepository<SheetMappingTemplate, Long> {
}