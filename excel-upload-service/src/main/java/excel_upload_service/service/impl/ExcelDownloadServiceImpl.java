// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/impl/ExcelDownloadServiceImpl.java
package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.model.RowEntity;
import excel_upload_service.model.SheetEntity;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.repository.SheetEntityRepository;
import excel_upload_service.service.ExcelDownloadService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelDownloadServiceImpl implements ExcelDownloadService {

    private final RowEntityRepository rowRepository;
    private final SheetEntityRepository sheetRepository; // NOUVEAU
    private final ObjectMapper objectMapper;

    public ExcelDownloadServiceImpl(RowEntityRepository rowRepository, SheetEntityRepository sheetRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.sheetRepository = sheetRepository; // NOUVEAU
        this.objectMapper = objectMapper;
    }

    @Override
    public void downloadSheetData(Long sheetId, String keyword, HttpServletResponse response) throws IOException {
        // 1. Récupérer l'entité feuille pour obtenir le nom et les en-têtes
        SheetEntity sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new EntityNotFoundException("Sheet not found with ID: " + sheetId));
        
        // 2. Récupérer toutes les lignes correspondantes pour cette feuille
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE); // Récupère tout
        List<RowEntity> results = rowRepository.searchBySheetIdAndKeyword(sheetId, keyword, pageable).getContent();

        if (results.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        // 3. Préparer la réponse HTTP
        String fileName = URLEncoder.encode(sheet.getSheetName() + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 4. Extraire les en-têtes depuis l'entité feuille
        List<String> headers = objectMapper.readValue(sheet.getHeadersJson(), new TypeReference<>() {});
        List<List<String>> excelHeaders = new ArrayList<>();
        headers.forEach(header -> excelHeaders.add(List.of(header)));

        // 5. Préparer les données pour l'écriture
        List<List<Object>> dataToWrite = new ArrayList<>();
        for (RowEntity entity : results) {
            Map<String, Object> rowData = objectMapper.readValue(entity.getDataJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
            List<Object> rowValues = new ArrayList<>();
            for (String header : headers) {
                rowValues.add(rowData.get(header));
            }
            dataToWrite.add(rowValues);
        }

        // 6. Écrire les données dans le fichier Excel
        EasyExcel.write(response.getOutputStream())
                .head(excelHeaders)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheet.getSheetName())
                .doWrite(dataToWrite);
    }
}