package excel_upload_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "row_entities", indexes = {
        @Index(name = "idx_sheet_index", columnList = "sheetIndex"),
        @Index(name = "idx_created_at", columnList = "createdAt")
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

   @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructeurs
    public RowEntity() {}

    public RowEntity(String dataJson, Integer sheetIndex, String fileName) {
        this.dataJson = dataJson;
        this.sheetIndex = sheetIndex;
        this.fileName = fileName;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }

    public Integer getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Integer sheetIndex) { this.sheetIndex = sheetIndex; }

   public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String dataJson;
        private Integer sheetIndex;
        private String fileName;

        public Builder dataJson(String dataJson) {
            this.dataJson = dataJson;
            return this;
        }

        public Builder sheetIndex(Integer sheetIndex) {
            this.sheetIndex = sheetIndex;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public RowEntity build() {
            return new RowEntity(dataJson, sheetIndex, fileName);
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
                ", fileName='" + fileName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}