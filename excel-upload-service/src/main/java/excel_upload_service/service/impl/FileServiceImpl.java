package excel_upload_service.service.impl;

import excel_upload_service.model.FileEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

    private final FileEntityRepository fileRepository;

    public FileServiceImpl(FileEntityRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public Page<FileEntity> getFiles(Pageable pageable) {
        return fileRepository.findAll(pageable);
    }
    
    @Override
    public FileEntity findById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }

    @Override
    public void deleteFile(Long id) {
        // The CascadeType.ALL on the relationship will handle deleting associated rows.
        fileRepository.deleteById(id);
    }
}