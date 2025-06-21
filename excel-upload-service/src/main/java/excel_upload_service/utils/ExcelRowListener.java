package excel_upload_service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.RowEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public class ExcelRowListener extends AnalysisEventListener<Map<Integer, String>> {

    private static final Logger logger = LoggerFactory.getLogger(ExcelRowListener.class);
    private static final int BATCH_SIZE = 100; // Traitement par batch pour optimiser les performances

    private final List<RowEntity> entityBatch = new ArrayList<>();
    private final List<String> headers;
    private final RowEntityRepository repository;
    private final ObjectMapper objectMapper;
    private final int sheetIndex;
    private int currentRowIndex = 0;

    public ExcelRowListener(List<String> headers,
                            RowEntityRepository repository,
                            ObjectMapper objectMapper,
                            int sheetIndex) {
        this.headers = Objects.requireNonNull(headers, "Headers ne peuvent pas être null");
        this.repository = Objects.requireNonNull(repository, "Repository ne peut pas être null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper ne peut pas être null");
        this.sheetIndex = sheetIndex;

        if (headers.isEmpty()) {
            throw new IllegalArgumentException("La liste des headers ne peut pas être vide");
        }
    }

    @Override
    public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
        try {
            currentRowIndex++;

            // Vérifier si la ligne n'est pas complètement vide
            if (isRowEmpty(rowData)) {
                logger.debug("Ligne vide ignorée à l'index: {}", currentRowIndex);
                return;
            }

            Map<String, Object> structuredRow = buildStructuredRow(rowData);
            String json = objectMapper.writeValueAsString(structuredRow);

            RowEntity entity = RowEntity.builder()
                    .dataJson(json)
                    .sheetIndex(sheetIndex)
                    .build();

            entityBatch.add(entity);

            // Sauvegarder par batch pour améliorer les performances
            if (entityBatch.size() >= BATCH_SIZE) {
                saveBatch();
            }

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la ligne {}: {}", currentRowIndex, e.getMessage(), e);
            // Selon votre logique métier, vous pouvez choisir de continuer ou d'arrêter
            // throw new RuntimeException("Erreur de conversion JSON à la ligne " + currentRowIndex, e);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Sauvegarder le dernier batch s'il reste des données
        if (!entityBatch.isEmpty()) {
            saveBatch();
        }
        logger.info("Traitement terminé. {} lignes traitées pour la feuille {}", currentRowIndex, sheetIndex);
    }

    /**
     * Construit la structure de données avec les headers comme clés
     */
    private Map<String, Object> buildStructuredRow(Map<Integer, String> rowData) {
        Map<String, Object> structuredRow = new LinkedHashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String value = rowData.getOrDefault(i, null);
            // Nettoyer les valeurs (trim, null si vide)
            value = (value != null && !value.trim().isEmpty()) ? value.trim() : null;
            structuredRow.put(headers.get(i), value);
        }

        return structuredRow;
    }

    /**
     * Vérifie si une ligne est complètement vide
     */
    private boolean isRowEmpty(Map<Integer, String> rowData) {
        if (rowData == null || rowData.isEmpty()) {
            return true;
        }

        return rowData.values().stream()
                .allMatch(value -> value == null || value.trim().isEmpty());
    }

    /**
     * Sauvegarde le batch actuel et vide la liste
     */
    private void saveBatch() {
        try {
            repository.saveAll(entityBatch);
            logger.debug("Batch de {} entités sauvegardé", entityBatch.size());
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde du batch: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la sauvegarde des données", e);
        } finally {
            entityBatch.clear();
        }
    }

    /**
     * Méthode pour obtenir des statistiques de traitement
     */
    public int getProcessedRowsCount() {
        return currentRowIndex;
    }
}