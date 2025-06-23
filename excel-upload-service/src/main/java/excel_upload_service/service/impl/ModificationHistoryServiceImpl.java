// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/ModificationHistoryServiceImpl.java
package excel_upload_service.service.impl;

import excel_upload_service.model.ModificationHistory;
import excel_upload_service.repository.ModificationHistoryRepository;
import excel_upload_service.service.ModificationHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModificationHistoryServiceImpl implements ModificationHistoryService {

    private final ModificationHistoryRepository repository;

    public ModificationHistoryServiceImpl(ModificationHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveHistory(Long rowId, String operationType, String oldData, String newData) {
        ModificationHistory history = new ModificationHistory(
                null, rowId, operationType, oldData, newData, LocalDateTime.now()
        );
        repository.save(history);
    }

    @Override
    public List<ModificationHistory> getHistoryForRow(Long rowId) {
        return repository.findByRowEntityIdOrderByTimestampDesc(rowId);
    }
    
    @Override
    public List<ModificationHistory> getAllHistories() {
        return repository.findAllByOrderByTimestampDesc();
    }
    
    // NOUVELLE MÉTHODE
    @Override
    public Page<ModificationHistory> getHistoryForSheet(Long sheetId, Pageable pageable) {
        return repository.findHistoryForSheet(sheetId, pageable);
    }
}