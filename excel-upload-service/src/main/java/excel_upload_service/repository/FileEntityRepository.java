package excel_upload_service.repository;

import excel_upload_service.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {
    Page<FileEntity> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);
}