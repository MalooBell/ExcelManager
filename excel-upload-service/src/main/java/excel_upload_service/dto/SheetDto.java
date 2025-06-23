// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/SheetDto.java
package excel_upload_service.dto;

import java.io.Serializable;

public class SheetDto implements Serializable {
    private Long id;
    private String sheetName;
    private int sheetIndex;
    private String headersJson;
    private long totalRows;

    // --- Constructeurs, Getters et Setters ---
    public SheetDto() {}

    public SheetDto(Long id, String sheetName, int sheetIndex, String headersJson, long totalRows) {
        this.id = id;
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.headersJson = headersJson;
        this.totalRows = totalRows;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public int getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }
    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
}