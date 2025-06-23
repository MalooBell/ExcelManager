// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/repository/ModificationHistoryRepository.java
package excel_upload_service.repository;

import excel_upload_service.model.ModificationHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModificationHistoryRepository extends JpaRepository<ModificationHistory, Long> {

    List<ModificationHistory> findByRowEntityIdOrderByTimestampDesc(Long rowEntityId);
    
    List<ModificationHistory> findAllByOrderByTimestampDesc();

    // NOUVELLE MÉTHODE : Récupère l'historique paginé pour une feuille donnée
    @Query("SELECT mh FROM ModificationHistory mh WHERE mh.rowEntityId IN " +
           "(SELECT r.id FROM RowEntity r WHERE r.sheet.id = :sheetId) " +
           "ORDER BY mh.timestamp DESC")
    Page<ModificationHistory> findHistoryForSheet(@Param("sheetId") Long sheetId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM ModificationHistory")
    void deleteAllFast();

    @Modifying
    @Transactional
    @Query("DELETE FROM ModificationHistory mh WHERE mh.rowEntityId IN :rowIds")
    void deleteByRowEntityIds(@Param("rowIds") List<Long> rowIds);
}