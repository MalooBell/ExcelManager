package excel_upload_service.repository;




import excel_upload_service.model.ModificationHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModificationHistoryRepository extends JpaRepository<ModificationHistory, Long> {
    List<ModificationHistory> findByRowEntityIdOrderByTimestampDesc(Long rowEntityId);
    List<ModificationHistory> findAllByOrderByTimestampDesc();
    @Modifying
    @Transactional
    @Query("DELETE FROM ModificationHistory")
    void deleteAllFast(/*Pageable pageable*/);
}

