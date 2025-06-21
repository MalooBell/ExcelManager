package excel_upload_service.service.impl;

import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.ModificationHistoryRepository;
import excel_upload_service.service.ResetService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResetServiceImpl implements ResetService {

    private final RowEntityRepository rowRepository;
    private final ModificationHistoryRepository historyRepository;

    public ResetServiceImpl(RowEntityRepository rowRepository, ModificationHistoryRepository historyRepository) {
        this.rowRepository = rowRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional
    public void resetAll() {
        /*historyRepository.deleteAll();
        rowRepository.deleteAll();*/
        historyRepository.deleteAllFast();
        rowRepository.deleteAllFast();
    }
}
