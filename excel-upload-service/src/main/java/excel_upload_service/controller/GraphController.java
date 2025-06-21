package excel_upload_service.controller;

import excel_upload_service.dto.GraphRequestDto;
import excel_upload_service.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/graphs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping("/file/{fileId}")
    public ResponseEntity<Map<String, Object>> generateGraphData(
            @PathVariable Long fileId,
            @RequestBody GraphRequestDto request) throws IOException {

        Map<String, Object> graphData = graphService.generateChartData(fileId, request);
        return ResponseEntity.ok(graphData);
    }
}