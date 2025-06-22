package excel_upload_service.service.impl;

import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.dto.GraphResult;
import excel_upload_service.dto.GroupedGraphResult;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.GraphService;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    private final RowEntityRepository rowRepository;
    private final ObjectMapper objectMapper;

    public GraphServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException {
        String chartType = request.getChartType();
        String aggregationType = request.getAggregationType();
        String groupingColumn = request.getGroupingColumn();
        Integer limit = request.getLimit() == null ? 100 : request.getLimit(); // Valeur par défaut de 50 si non fournie

        if ("bar".equalsIgnoreCase(chartType) && groupingColumn != null && !groupingColumn.isBlank()) {
            return generateGroupedBarChartData(fileId, request.getCategoryColumn(), groupingColumn, limit);
        }

        if ("pie".equalsIgnoreCase(chartType)) {
            return generateCategoryCountData(fileId, request.getCategoryColumn(), limit);
        }

        if ("bar".equalsIgnoreCase(chartType)) {
            if ("SUM".equalsIgnoreCase(aggregationType)) {
                return generateBarChartSumData(fileId, request.getCategoryColumn(), request.getValueColumns(), limit);
            } else {
                return generateCategoryCountData(fileId, request.getCategoryColumn(), limit);
            }
        }

        throw new IllegalArgumentException("Unsupported chart type: " + chartType);
    }

    private Map<String, Object> generateCategoryCountData(Long fileId, String categoryColumn, Integer limit) {
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }
        String jsonPath = "$." + categoryColumn;
        List<GraphResult> results = rowRepository.getCategoryCountsForGraph(fileId, jsonPath, limit == null ? 1000 : limit);

        List<String> labels = results.stream().map(GraphResult::getCategory).collect(Collectors.toList());
        List<BigDecimal> data = results.stream().map(GraphResult::getCount).collect(Collectors.toList());

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", data, "label", "Nombre d'occurrences")));
        return chartData;
    }

    private Map<String, Object> generateGroupedBarChartData(Long fileId, String primaryCategoryColumn, String secondaryCategoryColumn, Integer limit) {
        String primaryPath = "$." + primaryCategoryColumn;
        String secondaryPath = "$." + secondaryCategoryColumn;

        List<GroupedGraphResult> results = rowRepository.getGroupedCategoryCounts(fileId, primaryPath, secondaryPath, limit == null ? 100 : limit);

        // 1. Extraire les labels uniques pour les deux axes
        List<String> primaryLabels = results.stream().map(GroupedGraphResult::getPrimaryCategory).distinct().sorted().collect(Collectors.toList());
        List<String> secondaryLabels = results.stream().map(GroupedGraphResult::getSecondaryCategory).distinct().sorted().collect(Collectors.toList());

        // 2. Préparer les datasets (un par catégorie secondaire)
        Map<String, Map<String, Object>> datasetsMap = new LinkedHashMap<>();
        for (String secondaryLabel : secondaryLabels) {
            Map<String, Object> dataset = new LinkedHashMap<>();
            dataset.put("label", secondaryLabel);
            // Initialiser les données à 0 pour chaque label primaire
            BigDecimal[] data = new BigDecimal[primaryLabels.size()];
            Arrays.fill(data, BigDecimal.ZERO);
            dataset.put("data", data);
            datasetsMap.put(secondaryLabel, dataset);
        }

        // 3. Remplir les données
        for (GroupedGraphResult result : results) {
            int primaryIndex = primaryLabels.indexOf(result.getPrimaryCategory());
            if (primaryIndex != -1) {
                Map<String, Object> dataset = datasetsMap.get(result.getSecondaryCategory());
                ((BigDecimal[]) dataset.get("data"))[primaryIndex] = result.getValue();
            }
        }

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", primaryLabels);
        chartData.put("datasets", new ArrayList<>(datasetsMap.values()));

        return chartData;
    }

    private Map<String, Object> generateBarChartSumData(Long fileId, String categoryColumn, List<String> valueColumns, Integer limit) {
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }
        if (valueColumns == null || valueColumns.isEmpty()) {
            throw new IllegalArgumentException("Au moins une colonne de valeur est requise pour une somme.");
        }

        String categoryJsonPath = "$." + categoryColumn;

        List<GraphResult> initialResults = rowRepository.getCategorySumsForGraph(fileId, categoryJsonPath, "$." + valueColumns.get(0), limit == null ? 1000 : limit);
        List<String> labels = initialResults.stream()
                                            .map(GraphResult::getCategory)
                                            .sorted()
                                            .collect(Collectors.toList());

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (String valueCol : valueColumns) {
            String valueJsonPath = "$." + valueCol;
            List<GraphResult> results = rowRepository.getCategorySumsForGraph(fileId, categoryJsonPath, valueJsonPath, limit == null ? 1000 : limit);
            
            Map<String, BigDecimal> resultMap = results.stream()
                .collect(Collectors.toMap(GraphResult::getCategory, GraphResult::getCount, (v1, v2) -> v1));
            
            List<BigDecimal> dataPoints = labels.stream()
                                          .map(label -> resultMap.getOrDefault(label, BigDecimal.ZERO))
                                          .collect(Collectors.toList());
            
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