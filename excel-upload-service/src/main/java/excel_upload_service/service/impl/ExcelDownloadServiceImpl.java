package excel_upload_service.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.model.RowEntity;
import excel_upload_service.repository.RowEntityRepository;
import excel_upload_service.service.ExcelDownloadService;
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
    private final ObjectMapper objectMapper;

    public ExcelDownloadServiceImpl(RowEntityRepository rowRepository, ObjectMapper objectMapper) {
        this.rowRepository = rowRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void downloadFilteredData(String fileName, String keyword, HttpServletResponse response) throws IOException {
        // On récupère toutes les données correspondantes (sans pagination pour le téléchargement)
        // ATTENTION: Pour de très gros volumes, une approche par streaming serait plus robuste.
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE); // Récupère tout
        List<RowEntity> results = rowRepository.search(fileName, keyword, pageable).getContent();

        if (results.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        // Préparer la réponse HTTP pour un fichier Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encodedFileName = URLEncoder.encode("export.xlsx", StandardCharsets.UTF_8.toString());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");

        // Extraire les en-têtes de la première ligne de données
        // On utilise un LinkedHashMap pour préserver l'ordre des colonnes.
        Map<String, Object> firstRowData = objectMapper.readValue(results.get(0).getDataJson(), new TypeReference<>() {});
        List<String> headers = new ArrayList<>(firstRowData.keySet());

        List<List<Object>> dataToWrite = new ArrayList<>();
        for (RowEntity entity : results) {
            Map<String, Object> rowData = objectMapper.readValue(entity.getDataJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
            List<Object> rowValues = new ArrayList<>();
            for(String header : headers) {
                rowValues.add(rowData.get(header));
            }
            dataToWrite.add(rowValues);
        }

        // Créer les en-têtes pour EasyExcel (List<List<String>>)
        List<List<String>> excelHeaders = new ArrayList<>();
        headers.forEach(header -> excelHeaders.add(List.of(header)));

        // Écrire les données dans le flux de la réponse
        EasyExcel.write(response.getOutputStream())
                .head(excelHeaders)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // Ajuste la largeur des colonnes
                .sheet("Résultats")
                .doWrite(dataToWrite);
    }
}