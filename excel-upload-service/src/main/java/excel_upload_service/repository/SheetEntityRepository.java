// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/repository/SheetEntityRepository.java
package excel_upload_service.repository;

import excel_upload_service.model.SheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetEntityRepository extends JpaRepository<SheetEntity, Long> {
    List<SheetEntity> findByFileIdOrderBySheetIndexAsc(Long fileId);
}