// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/repository/SheetMappingRepository.java
package excel_upload_service.repository;

import excel_upload_service.model.SheetMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * NOUVEAU : Repository pour l'entité SheetMapping.
 * Il fournit les opérations CRUD (Create, Read, Update, Delete) de base.
 */
@Repository
public interface SheetMappingRepository extends JpaRepository<SheetMapping, Long> {

    /**
     * Méthode personnalisée pour trouver un mapping par l'ID de la feuille associée.
     * @param sheetId L'ID de l'entité SheetEntity.
     * @return Un Optional contenant le SheetMapping s'il existe.
     */
    Optional<SheetMapping> findBySheetId(Long sheetId);
}