// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/dto/python/SheetData.java
package excel_upload_service.dto.python;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SheetData {
    // Utilise @JsonProperty pour faire correspondre les noms de champs Python (snake_case)
    @JsonProperty("sheet_name")
    private String sheetName;
    
    private List<ColumnSchema> schema;
    private List<Map<String, Object>> data;
    
    @JsonProperty("total_rows")
    private int totalRows;

    // Getters et Setters
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public List<ColumnSchema> getSchema() { return schema; }
    public void setSchema(List<ColumnSchema> schema) { this.schema = schema; }
    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
}