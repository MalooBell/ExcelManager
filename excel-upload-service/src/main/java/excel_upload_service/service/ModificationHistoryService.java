// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/ModificationHistoryService.java
package excel_upload_service.service;

import excel_upload_service.model.ModificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ModificationHistoryService {
    void saveHistory(Long rowId, String operationType, String oldData, String newData);
    List<ModificationHistory> getHistoryForRow(Long rowId);
    List<ModificationHistory> getAllHistories();

    // NOUVELLE MÉTHODE
    Page<ModificationHistory> getHistoryForSheet(Long sheetId, Pageable pageable);
}