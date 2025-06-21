package excel_upload_service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.model.RowEntity;
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
    private final ObjectMapper objectMapper;

    public GraphServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException {
        List<RowEntity> rows = rowRepository.findByFileId(fileId);
        List<Map<String, Object>> data = new ArrayList<>();
        for (RowEntity row : rows) {
            data.add(objectMapper.readValue(row.getDataJson(), new TypeReference<>() {}));
        }

        switch (request.getChartType()) {
            case "pie":
            case "bar": // Bar chart with one value column is similar to a pie chart
                return generateCategoryCountData(data, request);
            // Case for 3D bar/multiple value columns can be added here
            default:
                throw new IllegalArgumentException("Unsupported chart type: " + request.getChartType());
        }
    }

    private Map<String, Object> generateCategoryCountData(List<Map<String, Object>> data, GraphRequestDto request) {
        // Group by the category column and count occurrences.
        Map<Object, Long> counts = data.stream()
                .filter(row -> row.get(request.getCategoryColumn()) != null)
                .collect(Collectors.groupingBy(
                        row -> row.get(request.getCategoryColumn()),
                        Collectors.counting()
                ));

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", counts.keySet());
        chartData.put("datasets", List.of(Map.of("data", counts.values())));
        return chartData;
    }

    // You can add more complex methods here, for example, for summing a value column
    // private Map<String, Object> generateCategorySumData(...) {}
}