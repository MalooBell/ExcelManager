package excel_upload_service.service.impl;

import excel_upload_service.dto.GraphCategoryCount; // Importer le DTO
import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.GraphService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    private final RowEntityRepository rowRepository;

    // Suppression de ObjectMapper ici car plus nécessaire pour cette logique
    public GraphServiceImpl(RowEntityRepository rowRepository) {
        this.rowRepository = rowRepository;
    }

    @Override
    public Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException {
        // La logique de chargement en mémoire est remplacée par une requête directe
        switch (request.getChartType()) {
            case "pie":
            case "bar":
                return generateCategoryCountDataFromDb(fileId, request);
            default:
                throw new IllegalArgumentException("Unsupported chart type: " + request.getChartType());
        }
    }

    // NOUVELLE MÉTHODE : utilise la requête native optimisée
    private Map<String, Object> generateCategoryCountDataFromDb(Long fileId, GraphRequestDto request) {
        String categoryColumn = request.getCategoryColumn();
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }

        // Construction du JSONPath pour la requête native
        String jsonPath = "$." + categoryColumn;

        List<GraphCategoryCount> results = rowRepository.getCategoryCountsForGraph(fileId, jsonPath);

        // Préparation des données pour Chart.js
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (GraphCategoryCount result : results) {
            labels.add(result.getCategory());
            data.add(result.getCount());
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", data, "label", "Nombre d'occurrences")));
        
        return chartData;
    }
}