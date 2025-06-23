// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/GraphService.java
package excel_upload_service.service;

import excel_upload_service.dto.GraphRequestDto;

import java.io.IOException;
import java.util.Map;

public interface GraphService {
    // CORRECTION : La méthode attend un sheetId, pas un fileId
    Map<String, Object> generateChartData(Long sheetId, GraphRequestDto request) throws IOException;
}