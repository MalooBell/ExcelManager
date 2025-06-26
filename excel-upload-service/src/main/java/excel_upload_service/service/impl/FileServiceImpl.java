// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/FileServiceImpl.java
package excel_upload_service.service.impl;

import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.ModificationHistoryRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.FileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

    private final FileEntityRepository fileRepository;
    private final RowEntityRepository rowRepository;
    private final ModificationHistoryRepository historyRepository;

    public FileServiceImpl(FileEntityRepository fileRepository, 
                           RowEntityRepository rowRepository, 
                           ModificationHistoryRepository historyRepository) {
        this.fileRepository = fileRepository;
        this.rowRepository = rowRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public Page<FileEntity> getFiles(String searchKeyword, Pageable pageable) {
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            return fileRepository.findByFileNameContainingIgnoreCase(searchKeyword, pageable);
        } else {
            return fileRepository.findAll(pageable);
        }
    }
    
    @Override
    public FileEntity findById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteFile(Long id) {
        if (!fileRepository.existsById(id)) {
            return; 
        }

        // CORRECTION : On utilise la nouvelle méthode pour trouver les lignes
        List<Long> rowIdsToDelete = rowRepository.findBySheetFileId(id)
                                                 .stream()
                                                 .map(RowEntity::getId)
                                                 .collect(Collectors.toList());
        
        if (!rowIdsToDelete.isEmpty()) {
            historyRepository.deleteByRowEntityIds(rowIdsToDelete);
        }

        rowRepository.deleteByFileId(id);

        fileRepository.deleteById(id);
    }
}