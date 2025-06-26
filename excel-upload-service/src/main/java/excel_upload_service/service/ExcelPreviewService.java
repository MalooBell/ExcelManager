// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/ExcelPreviewService.java
package excel_upload_service.service;

import excel_upload_service.dto.LayoutAnalysis; // Import LayoutAnalysis
import excel_upload_service.dto.SheetPreviewDto;
import java.io.IOException;
import java.io.InputStream; // Import InputStream

public interface ExcelPreviewService {
    SheetPreviewDto getSheetPreview(Long fileId, int sheetIndex, int rowLimit) throws IOException;

    // NEW METHOD: For analyzing sheet layout to detect header
    LayoutAnalysis analyzeSheetLayout(InputStream inputStream, int sheetIndex) throws IOException;
}