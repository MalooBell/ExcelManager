// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/GraphController.java
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

    // CORRECTION : L'endpoint utilise maintenant {sheetId}
    @PostMapping("/sheet/{sheetId}")
    public ResponseEntity<Map<String, Object>> generateGraphData(
            @PathVariable Long sheetId,
            @RequestBody GraphRequestDto request) throws IOException {

        Map<String, Object> graphData = graphService.generateChartData(sheetId, request);
        return ResponseEntity.ok(graphData);
    }
}