package excel_upload_service.service;

import excel_upload_service.dto.GraphRequestDto;

import java.io.IOException;
import java.util.Map;

public interface GraphService {
    Map<String, Object> generateChartData(Long fileId, GraphRequestDto request) throws IOException;
}