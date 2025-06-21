package excel_upload_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fileName;

    @Column(nullable = false)
    private LocalDateTime uploadTimestamp;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String headersJson; // Storing headers as JSON string

    private long totalRows;

    // One-to-many relationship: One file has many rows.
    // Cascade.ALL means if we delete a FileEntity, all its associated RowEntities are also deleted.
    // Fetch.LAZY is for performance, so rows are not loaded unless explicitly requested.
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<RowEntity> rows;

    @PrePersist
    protected void onCreate() {
        uploadTimestamp = LocalDateTime.now();
    }

    // --- Constructors, Getters, and Setters ---

    public FileEntity() {}

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

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public List<RowEntity> getRows() {
        return rows;
    }

    public void setRows(List<RowEntity> rows) {
        this.rows = rows;
    }
}