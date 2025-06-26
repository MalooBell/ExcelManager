// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/dto/python/ExcelProcessingResponse.java
package excel_upload_service.dto.python;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExcelProcessingResponse {
    @JsonProperty("file_name")
    private String fileName;
    
    private List<SheetData> sheets;

    // Getters et Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public List<SheetData> getSheets() { return sheets; }
    public void setSheets(List<SheetData> sheets) { this.sheets = sheets; }
}