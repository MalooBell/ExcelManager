package excel_upload_service.service;


import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ExcelDownloadService {
    void downloadFilteredData(
            String fileName,
            String keyword,
            HttpServletResponse response) throws IOException;
}
