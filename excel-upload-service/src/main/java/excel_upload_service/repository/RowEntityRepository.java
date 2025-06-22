package excel_upload_service.repository;



import excel_upload_service.dto.GraphCategoryCount;
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


    @Modifying
    @Transactional
    @Query("DELETE FROM RowEntity")
    void deleteAllFast(/*Pageable pageable*/);

    @Modifying
    @Transactional
    @Query("DELETE FROM RowEntity r WHERE r.file.id = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);

     @Query(value = "SELECT " +
                   "JSON_UNQUOTE(JSON_EXTRACT(data_json, :jsonPath)) as category, " +
                   "COUNT(*) as count " +
                   "FROM row_entities " +
                   "WHERE file_id = :fileId AND JSON_UNQUOTE(JSON_EXTRACT(data_json, :jsonPath)) IS NOT NULL " +
                   "GROUP BY category " +
                   "ORDER BY count DESC",
           nativeQuery = true)
    List<GraphCategoryCount> getCategoryCountsForGraph(
            @Param("fileId") Long fileId,
            @Param("jsonPath") String jsonPath
    );

    @Query("SELECT r FROM RowEntity r WHERE r.file.id = :fileId AND " +
            "(:keyword IS NULL OR r.dataJson LIKE %:keyword%)")
    Page<RowEntity> searchByFileIdAndKeyword(
            @Param("fileId") Long fileId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // This can be used to find all rows for a given file ID, which is useful for graphing.
    List<RowEntity> findByFileId(Long fileId);

    // You might want a more flexible search for the file dashboard
    @Query("SELECT r FROM RowEntity r WHERE " +
            "(:fileName IS NULL OR r.file.fileName LIKE %:fileName%) AND " +
            "(:keyword IS NULL OR r.dataJson LIKE %:keyword%)")
    Page<RowEntity> searchWithFileAndKeyword(
            @Param("fileName") String fileName,
            @Param("keyword") String keyword,
            Pageable pageable);



}
