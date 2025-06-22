package excel_upload_service.service.impl;

import excel_upload_service.model.FileEntity;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.repository.ModificationHistoryRepository;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.FileService;
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
    public Page<FileEntity> getFiles(Pageable pageable) {
        return fileRepository.findAll(pageable);
    }
    
    @Override
    public FileEntity findById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }

    @Override
    @Transactional // Assure que tout s'exécute dans une seule transaction
    public void deleteFile(Long id) {
        // 1. Vérifier si le fichier existe
        if (!fileRepository.existsById(id)) {
            // Optionnel : lancer une exception ou simplement retourner
            return; 
        }

        // 2. Récupérer tous les IDs des lignes associées au fichier
        List<Long> rowIdsToDelete = rowRepository.findByFileId(id)
                                                 .stream()
                                                 .map(RowEntity::getId)
                                                 .collect(Collectors.toList());

        // 3. Supprimer l'historique associé en masse (si des lignes existent)
        if (!rowIdsToDelete.isEmpty()) {
            historyRepository.deleteByRowEntityIds(rowIdsToDelete);
        }

        // 4. Supprimer les lignes associées en masse
        rowRepository.deleteByFileId(id);

        // 5. Supprimer l'entité fichier elle-même
        // Note : La cascade est maintenant inutile, mais il vaut mieux la laisser pour la cohérence
        fileRepository.deleteById(id);
    }
}