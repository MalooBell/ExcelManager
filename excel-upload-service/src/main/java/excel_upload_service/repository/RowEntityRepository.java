// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/repository/RowEntityRepository.java
package excel_upload_service.repository;

import excel_upload_service.dto.GraphResult;
import excel_upload_service.dto.GroupedGraphResult;
import excel_upload_service.model.RowEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // NOUVEAU
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MODIFIÉ : Le repository implémente maintenant JpaSpecificationExecutor.
 * DESCRIPTION : Cela nous permettra de construire des requêtes dynamiques et complexes
 * de manière plus sûre et plus propre que la concaténation de chaînes SQL.
 */
@Repository
public interface RowEntityRepository extends JpaRepository<RowEntity, Long>, JpaSpecificationExecutor<RowEntity> {

    // La méthode `searchBySheetIdAndKeyword` est maintenant gérée par la Spécification JPA,
    // nous n'avons plus besoin d'une requête @Query ici pour cette recherche spécifique.

    @Query("SELECT r FROM RowEntity r WHERE r.sheet.file.id = :fileId")
    List<RowEntity> findBySheetFileId(@Param("fileId") Long fileId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RowEntity")
    void deleteAllFast();

    @Modifying
    @Transactional
    @Query("DELETE FROM RowEntity r WHERE r.sheet.file.id = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);

    // Les requêtes pour les graphiques restent inchangées.
    @Query(value = "SELECT " +
                   "JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :jsonPath)) as category, " +
                   "COUNT(*) as count " +
                   "FROM row_entities r JOIN sheets s ON r.sheet_id = s.id " +
                   "WHERE s.id = :sheetId AND JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :jsonPath)) IS NOT NULL " +
                   "GROUP BY category " +
                   "ORDER BY count DESC LIMIT :limit",
           nativeQuery = true)
    List<GraphResult> getCategoryCountsForGraph(@Param("sheetId") Long sheetId, @Param("jsonPath") String jsonPath, @Param("limit") Integer limit);

    @Query(value = "WITH TopPrimaryCategories AS (" +
                   "  SELECT JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :primaryCategoryPath)) as p_category, COUNT(*) as total_count " +
                   "  FROM row_entities r JOIN sheets s ON r.sheet_id = s.id WHERE s.id = :sheetId GROUP BY p_category ORDER BY total_count DESC LIMIT :limit" +
                   ") " +
                   "SELECT " +
                   "  JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :primaryCategoryPath)) as primaryCategory, " +
                   "  JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :secondaryCategoryPath)) as secondaryCategory, " +
                   "  COUNT(*) as value " +
                   "FROM row_entities r " +
                   "JOIN TopPrimaryCategories tpc ON JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :primaryCategoryPath)) = tpc.p_category " +
                   "JOIN sheets s ON r.sheet_id = s.id " +
                   "WHERE s.id = :sheetId " +
                   "AND JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :secondaryCategoryPath)) IS NOT NULL " +
                   "GROUP BY primaryCategory, secondaryCategory " +
                   "ORDER BY primaryCategory, secondaryCategory",
           nativeQuery = true)
    List<GroupedGraphResult> getGroupedCategoryCounts(@Param("sheetId") Long sheetId, @Param("primaryCategoryPath") String primaryCategoryPath, @Param("secondaryCategoryPath") String secondaryCategoryPath, @Param("limit") Integer limit);

    @Query(value = "SELECT " +
                   "JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :jsonPath)) as category, " +
                   "SUM(CAST(JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :valueJsonPath)) AS DECIMAL(18, 4))) as count " +
                   "FROM row_entities r JOIN sheets s ON r.sheet_id = s.id " +
                   "WHERE s.id = :sheetId AND JSON_UNQUOTE(JSON_EXTRACT(r.data_json, :jsonPath)) IS NOT NULL " +
                   "GROUP BY category " +
                   "ORDER BY count DESC LIMIT :limit",
           nativeQuery = true)
    List<GraphResult> getCategorySumsForGraph(@Param("sheetId") Long sheetId, @Param("jsonPath") String jsonPath, @Param("valueJsonPath") String valueJsonPath, @Param("limit") Integer limit);
}