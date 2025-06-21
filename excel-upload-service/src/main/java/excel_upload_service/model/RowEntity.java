package excel_upload_service.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "row_entities", indexes = {
        @Index(name = "idx_sheet_index", columnList = "sheetIndex"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_file_id", columnList = "file_id") // Index for the foreign key
})
public class RowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String dataJson;

    @Column(nullable = false)
    private Integer sheetIndex;

    // Many-to-one relationship with FileEntity.
    // Fetch.LAZY is crucial for performance.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    @JsonBackReference // Prevents infinite recursion during serialization
    private FileEntity file;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- CONSTRUCTORS, GETTERS, SETTERS, BUILDER ---

    public RowEntity() {}

    // Updated constructor
    public RowEntity(String dataJson, Integer sheetIndex, FileEntity file) {
        this.dataJson = dataJson;
        this.sheetIndex = sheetIndex;
        this.file = file;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }

    public Integer getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Integer sheetIndex) { this.sheetIndex = sheetIndex; }

    public FileEntity getFile() { return file; }
    public void setFile(FileEntity file) { this.file = file; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataJson;
        private Integer sheetIndex;
        private FileEntity file;

        public Builder dataJson(String dataJson) {
            this.dataJson = dataJson;
            return this;
        }

        public Builder sheetIndex(Integer sheetIndex) {
            this.sheetIndex = sheetIndex;
            return this;
        }

        public Builder file(FileEntity file) {
            this.file = file;
            return this;
        }

        public RowEntity build() {
            return new RowEntity(dataJson, sheetIndex, file);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowEntity that = (RowEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RowEntity{" +
                "id=" + id +
                ", sheetIndex=" + sheetIndex +
                ", fileId=" + (file != null ? file.getId() : "null") +
                ", createdAt=" + createdAt +
                '}';
    }
}