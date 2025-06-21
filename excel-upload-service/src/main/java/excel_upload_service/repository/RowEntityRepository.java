package excel_upload_service.repository;



import excel_upload_service.model.RowEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RowEntityRepository extends JpaRepository<RowEntity, Long> {

    List<RowEntity> findByFileName(String fileName);

    List<RowEntity> findBySheetIndex(Integer sheetIndex);

   @Query("SELECT r FROM RowEntity r WHERE r.fileName = :fileName AND r.sheetIndex = :sheetIndex")
    List<RowEntity> findByFileNameAndSheetIndex(@Param("fileName") String fileName, @Param("sheetIndex") Integer sheetIndex);

    @Query("SELECT r FROM RowEntity r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<RowEntity> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // NOUVELLE MÉTHODE DE RECHERCHE
    @Query("SELECT r FROM RowEntity r WHERE " +
            "(:fileName IS NULL OR r.fileName LIKE %:fileName%) AND " +
            "(:keyword IS NULL OR r.dataJson LIKE %:keyword%)")
    Page<RowEntity> search(
            @Param("fileName") String fileName,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM RowEntity")
    void deleteAllFast(/*Pageable pageable*/);
}
