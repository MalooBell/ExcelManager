package excel_upload_service.service.impl;

import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.dto.GraphResult;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.GraphService;
import org.springframework.stereotype.Service;
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
    private final ObjectMapper objectMapper;

    public GraphServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException {
        String chartType = request.getChartType();
        String aggregationType = request.getAggregationType();

        if ("pie".equalsIgnoreCase(chartType)) {
            return generateCategoryCountData(fileId, request.getCategoryColumn());
        }

        if ("bar".equalsIgnoreCase(chartType)) {
            if ("SUM".equalsIgnoreCase(aggregationType)) {
                return generateBarChartSumData(fileId, request.getCategoryColumn(), request.getValueColumns());
            } else {
                return generateCategoryCountData(fileId, request.getCategoryColumn());
            }
        }

        throw new IllegalArgumentException("Unsupported chart type: " + chartType);
    }

    private Map<String, Object> generateCategoryCountData(Long fileId, String categoryColumn) {
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }
        String jsonPath = "$." + categoryColumn;
        List<GraphResult> results = rowRepository.getCategoryCountsForGraph(fileId, jsonPath);

        List<String> labels = results.stream().map(GraphResult::getCategory).collect(Collectors.toList());
        List<BigDecimal> data = results.stream().map(GraphResult::getCount).collect(Collectors.toList());

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", List.of(Map.of("data", data, "label", "Nombre d'occurrences")));
        return chartData;
    }

    private Map<String, Object> generateBarChartSumData(Long fileId, String categoryColumn, List<String> valueColumns) {
        if (categoryColumn == null || categoryColumn.isBlank()) {
            throw new IllegalArgumentException("La colonne de catégorie est requise.");
        }
        if (valueColumns == null || valueColumns.isEmpty()) {
            throw new IllegalArgumentException("Au moins une colonne de valeur est requise pour une somme.");
        }

        String categoryJsonPath = "$." + categoryColumn;

        List<GraphResult> initialResults = rowRepository.getCategorySumsForGraph(fileId, categoryJsonPath, "$." + valueColumns.get(0));
        List<String> labels = initialResults.stream()
                                            .map(GraphResult::getCategory)
                                            .sorted()
                                            .collect(Collectors.toList());

        List<Map<String, Object>> datasets = new ArrayList<>();

        for (String valueCol : valueColumns) {
            String valueJsonPath = "$." + valueCol;
            List<GraphResult> results = rowRepository.getCategorySumsForGraph(fileId, categoryJsonPath, valueJsonPath);
            
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