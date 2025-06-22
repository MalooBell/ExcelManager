package excel_upload_service.service.impl;

import excel_upload_service.dto.GraphCategoryCount; // Importer le DTO
import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.GraphService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    private final RowEntityRepository rowRepository;
    private final ObjectMapper objectMapper; // <-- AJOUTER CETTE LIGNE


    // Suppression de ObjectMapper ici car plus nécessaire pour cette logique
    public GraphServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper; // <-- AJOUTER CETTE LIGNE
    }

    @Override
public Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException {
    // La logique de chargement en mémoire est remplacée par une requête directe
    switch (request.getChartType()) {
        case "pie":
            return generateCategoryCountDataFromDb(fileId, request);
        case "bar":
            // Appeler la nouvelle méthode pour les graphiques en barres
            return generateBarChartDataFromDb(fileId, request);
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

    // NOUVELLE MÉTHODE pour les graphiques en barres
private Map<String, Object> generateBarChartDataFromDb(Long fileId, GraphRequestDto request) throws IOException {
    String categoryColumn = request.getCategoryColumn();
    List<String> valueColumns = request.getValueColumns();

    if (categoryColumn == null || categoryColumn.isBlank()) {
        throw new IllegalArgumentException("La colonne de catégorie est requise.");
    }
    if (valueColumns == null || valueColumns.isEmpty()) {
        throw new IllegalArgumentException("Au moins une colonne de valeur est requise pour un graphique en barres.");
    }

    // 1. Récupérer toutes les lignes pour le fichier
    List<RowEntity> rows = rowRepository.findByFileId(fileId);
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

    // 2. Agréger les données en mémoire
    // Structure : Map<Category, Map<ValueColumnName, Sum>>
    Map<String, Map<String, BigDecimal>> aggregatedData = new LinkedHashMap<>();

    for (RowEntity row : rows) {
        Map<String, Object> data = objectMapper.readValue(row.getDataJson(), typeRef);
        Object categoryValue = data.get(categoryColumn);
        // Utiliser "N/A" pour les catégories nulles ou vides
        String category = (categoryValue != null && !categoryValue.toString().isBlank()) ? categoryValue.toString() : "N/A";

        aggregatedData.putIfAbsent(category, new LinkedHashMap<>());

        for (String valueCol : valueColumns) {
            Object rawValue = data.get(valueCol);
            if (rawValue != null) {
                try {
                    // Tenter de convertir la valeur en nombre, en gérant les virgules et les espaces
                    BigDecimal numericValue = new BigDecimal(rawValue.toString().trim().replace(',', '.'));
                    // Ajouter à la somme existante pour cette catégorie et cette colonne
                    aggregatedData.get(category).merge(valueCol, numericValue, BigDecimal::add);
                } catch (NumberFormatException e) {
                    // Ignorer en silence les valeurs qui ne sont pas des nombres
                }
            }
        }
    }

    // 3. Préparer les données pour la réponse à Chart.js
    List<String> labels = new ArrayList<>(aggregatedData.keySet());
    List<Map<String, Object>> datasets = new ArrayList<>();

    for (String valueCol : valueColumns) {
        List<BigDecimal> dataPoints = new ArrayList<>();
        for (String label : labels) {
            // Obtenir la somme pour cette catégorie/colonne, ou 0 si elle n'existe pas
            BigDecimal value = aggregatedData.get(label).getOrDefault(valueCol, BigDecimal.ZERO);
            dataPoints.add(value);
        }

        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("label", valueCol);
        dataset.put("data", dataPoints);
        datasets.add(dataset);
    }

    Map<String, Object> chartData = new LinkedHashMap<>();
    chartData.put("labels", labels);
    chartData.put("datasets", datasets);

    return chartData;
}
}