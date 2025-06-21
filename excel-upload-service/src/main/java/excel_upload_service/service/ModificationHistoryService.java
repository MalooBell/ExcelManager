package excel_upload_service.service;


import java.util.List;
import excel_upload_service.model.ModificationHistory;

public interface ModificationHistoryService {
    void saveHistory(Long rowId, String operationType, String oldData, String newData);
    List<ModificationHistory> getHistoryForRow(Long rowId);
    public List<ModificationHistory> getAllHistories();
    
}

