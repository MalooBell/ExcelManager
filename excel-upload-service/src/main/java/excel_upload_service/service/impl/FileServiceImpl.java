// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/FileServiceImpl.java
package excel_upload_service.service.impl;

import excel_upload_service.dto.FileDto;
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

   /**
     * MODIFIÉ : Retourne maintenant une Page<FileDto>
     */
    @Override
    public Page<FileDto> getFiles(String searchKeyword, Pageable pageable) {
        Page<FileEntity> filePage;
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            filePage = fileRepository.findByFileNameContainingIgnoreCase(searchKeyword, pageable);
        } else {
            filePage = fileRepository.findAll(pageable);
        }
        // On utilise la méthode .map() de Page pour convertir chaque FileEntity en FileDto.
        return filePage.map(this::convertToFileDto);
    }
    
    /**
     * NOUVEAU : Méthode privée pour convertir une entité en DTO.
     */
    private FileDto convertToFileDto(FileEntity fileEntity) {
        FileDto dto = new FileDto();
        dto.setId(fileEntity.getId());
        dto.setFileName(fileEntity.getFileName());
        dto.setUploadTimestamp(fileEntity.getUploadTimestamp());
        // Récupère le nombre de feuilles sans charger la collection entière.
        dto.setSheetCount(fileEntity.getSheets() != null ? fileEntity.getSheets().size() : 0);
        return dto;
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