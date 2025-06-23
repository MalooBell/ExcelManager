// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/GraphServiceImpl.java
package excel_upload_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.dto.GraphResult;
import excel_upload_service.dto.GroupedGraphResult;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.GraphService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphServiceImpl implements GraphService {

    private final RowEntityRepository rowRepository;
    private final ObjectMapper objectMapper;

    public GraphServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    // CORRECTION : Le paramètre est maintenant sheetId
    @Override
    public Map<String, Object> generateChartData(Long sheetId, GraphRequestDto request) throws IOException {
        String chartType = request.getChartType();
        String aggregationType = request.getAggregationType();
        String groupingColumn = request.getGroupingColumn();
        Integer limit = request.getLimit() == null ? 100 : request.getLimit();

        if ("bar".equalsIgnoreCase(chartType) && groupingColumn != null && !groupingColumn.isBlank()) {
            return generateGroupedBarChartData(sheetId, request.getCategoryColumn(), groupingColumn, limit);
        }

        if ("pie".equalsIgnoreCase(chartType)) {
            return generateCategoryCountData(sheetId, request.getCategoryColumn(), limit);
        }

        if ("bar".equalsIgnoreCase(chartType)) {
            if ("SUM".equalsIgnoreCase(aggregationType)) {
                return generateBarChartSumData(sheetId, request.getCategoryColumn(), request.getValueColumns(), limit);
            } else {
                return generateCategoryCountData(sheetId, request.getCategoryColumn(), limit);
            }
        }

        throw new IllegalArgumentException("Unsupported chart type: " + chartType);
    }

    private Map<String, Object> generateCategoryCountData(Long sheetId, String categoryColumn, Integer limit) {
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }
        String jsonPath = "$." + categoryColumn;
        // CORRECTION : On passe sheetId au repository
        List<GraphResult> results = rowRepository.getCategoryCountsForGraph(sheetId, jsonPath, limit == null ? 1000 : limit);

        List<String> labels = results.stream().map(GraphResult::getCategory).collect(Collectors.toList());
        List<BigDecimal> data = results.stream().map(GraphResult::getCount).collect(Collectors.toList());

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", data, "label", "Nombre d'occurrences")));
        return chartData;
    }

    private Map<String, Object> generateGroupedBarChartData(Long sheetId, String primaryCategoryColumn, String secondaryCategoryColumn, Integer limit) {
        String primaryPath = "$." + primaryCategoryColumn;
        String secondaryPath = "$." + secondaryCategoryColumn;
        // CORRECTION : On passe sheetId au repository
        List<GroupedGraphResult> results = rowRepository.getGroupedCategoryCounts(sheetId, primaryPath, secondaryPath, limit == null ? 100 : limit);

        // ... Le reste de la logique de cette méthode est inchangé ...
        List<String> primaryLabels = results.stream().map(GroupedGraphResult::getPrimaryCategory).distinct().sorted().collect(Collectors.toList());
        List<String> secondaryLabels = results.stream().map(GroupedGraphResult::getSecondaryCategory).distinct().sorted().collect(Collectors.toList());
        Map<String, Map<String, Object>> datasetsMap = new LinkedHashMap<>();
        for (String secondaryLabel : secondaryLabels) {
            Map<String, Object> dataset = new LinkedHashMap<>();
            dataset.put("label", secondaryLabel);
            BigDecimal[] data = new BigDecimal[primaryLabels.size()];
            Arrays.fill(data, BigDecimal.ZERO);
            dataset.put("data", data);
            datasetsMap.put(secondaryLabel, dataset);
        }
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

    private Map<String, Object> generateBarChartSumData(Long sheetId, String categoryColumn, List<String> valueColumns, Integer limit) {
        if (categoryColumn == null || categoryColumn.isBlank()) throw new IllegalArgumentException("La colonne de catégorie est requise.");
        if (valueColumns == null || valueColumns.isEmpty()) throw new IllegalArgumentException("Au moins une colonne de valeur est requise pour une somme.");
        
        String categoryJsonPath = "$." + categoryColumn;
        // CORRECTION : On passe sheetId au repository
        List<GraphResult> initialResults = rowRepository.getCategorySumsForGraph(sheetId, categoryJsonPath, "$." + valueColumns.get(0), limit == null ? 1000 : limit);
        List<String> labels = initialResults.stream().map(GraphResult::getCategory).sorted().collect(Collectors.toList());
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        for (String valueCol : valueColumns) {
            String valueJsonPath = "$." + valueCol;
            // CORRECTION : On passe sheetId au repository
            List<GraphResult> results = rowRepository.getCategorySumsForGraph(sheetId, categoryJsonPath, valueJsonPath, limit == null ? 1000 : limit);
            Map<String, BigDecimal> resultMap = results.stream().collect(Collectors.toMap(GraphResult::getCategory, GraphResult::getCount, (v1, v2) -> v1));
            List<BigDecimal> dataPoints = labels.stream().map(label -> resultMap.getOrDefault(label, BigDecimal.ZERO)).collect(Collectors.toList());
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