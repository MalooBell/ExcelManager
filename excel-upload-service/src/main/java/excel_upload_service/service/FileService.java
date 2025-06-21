package excel_upload_service.service;

import excel_upload_service.model.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FileService {
    Page<FileEntity> getFiles(Pageable pageable);
    void deleteFile(Long id);
    FileEntity findById(Long id);
}