// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/ExcelDownloadService.java
package excel_upload_service.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ExcelDownloadService {

    /**
     * Télécharge les données d'une feuille spécifique, potentiellement filtrées par un mot-clé.
     */
    void downloadSheetData(
            Long sheetId,
            String keyword,
            HttpServletResponse response) throws IOException;
}