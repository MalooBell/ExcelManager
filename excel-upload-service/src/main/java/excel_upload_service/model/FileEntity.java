// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/model/FileEntity.java
package excel_upload_service.model;

import jakarta.persistence.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp;

    @Column(name = "total_processed_rows")
    private int totalProcessedRows;

    @Column(name = "processed") // <--- NEW FIELD
    private boolean processed;

    @Column(name = "needs_header_validation") // <--- NEW FIELD
    private boolean needsHeaderValidation;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SheetEntity> sheets;

    public FileEntity() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public int getTotalProcessedRows() {
        return totalProcessedRows;
    }

    public void setTotalProcessedRows(int totalProcessedRows) {
        this.totalProcessedRows = totalProcessedRows;
    }

    public List<SheetEntity> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetEntity> sheets) {
        this.sheets = sheets;
    }

    public boolean isProcessed() { // <--- NEW GETTER
        return processed;
    }

    public void setProcessed(boolean processed) { // <--- NEW SETTER
        this.processed = processed;
    }

    public boolean isNeedsHeaderValidation() { // <--- NEW GETTER
        return needsHeaderValidation;
    }

    public void setNeedsHeaderValidation(boolean needsHeaderValidation) { // <--- NEW SETTER
        this.needsHeaderValidation = needsHeaderValidation;
    }
    
    // NEW: Method to provide an InputStream from the stored file
    // This is crucial for re-reading the file in various services (like ExcelPreviewService or reprocessing)
    public InputStream getStoredFileStream(Path fileStorageLocation) {
        try {
            // Assuming fileName includes the timestamp prefix, which makes it unique and directly maps to the stored file name
            Path filePath = fileStorageLocation.resolve(this.fileName);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Stored file not found: " + filePath.toString());
            }
            return Files.newInputStream(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Error getting stored file stream for " + this.fileName + ": " + e.getMessage(), e);
        }
    }
}