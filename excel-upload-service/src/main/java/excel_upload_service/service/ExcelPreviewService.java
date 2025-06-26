// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/ExcelPreviewService.java
package excel_upload_service.service;

import excel_upload_service.dto.SheetPreviewDto;
import java.io.IOException;

public interface ExcelPreviewService {
    SheetPreviewDto getSheetPreview(Long fileId, int sheetIndex, int rowLimit) throws IOException;
}