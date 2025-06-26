// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/model/SheetEntity.java
package excel_upload_service.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "sheets")
public class SheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "sheet_name", nullable = false)
    private String sheetName;

    @Column(name = "sheet_index", nullable = false)
    private int sheetIndex;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "header_row_index") // <--- NEW FIELD
    private int headerRowIndex;

    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RowEntity> rows;

    public SheetEntity() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileEntity getFile() {
        return file;
    }

    public void setFile(FileEntity file) {
        this.file = file;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public int getHeaderRowIndex() { // <--- NEW GETTER
        return headerRowIndex;
    }

    public void setHeaderRowIndex(int headerRowIndex) { // <--- NEW SETTER
        this.headerRowIndex = headerRowIndex;
    }

    public List<RowEntity> getRows() {
        return rows;
    }

    public void setRows(List<RowEntity> rows) {
        this.rows = rows;
    }
}