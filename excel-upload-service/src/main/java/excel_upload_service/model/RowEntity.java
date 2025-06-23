// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/model/RowEntity.java
package excel_upload_service.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "row_entities", indexes = {
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_sheet_id", columnList = "sheet_id") // Index sur la nouvelle clé étrangère
})
public class RowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String dataJson;

    // La relation est maintenant avec SheetEntity
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sheet_id", nullable = false)
    @JsonBackReference
    private SheetEntity sheet;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- CONSTRUCTEURS, GETTERS, SETTERS, BUILDER ---
    public RowEntity() {}

    public RowEntity(String dataJson, SheetEntity sheet) {
        this.dataJson = dataJson;
        this.sheet = sheet;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public SheetEntity getSheet() { return sheet; }
    public void setSheet(SheetEntity sheet) { this.sheet = sheet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

     public static class Builder {
        private String dataJson;
        private SheetEntity sheet;

        public Builder dataJson(String dataJson) {
            this.dataJson = dataJson;
            return this;
        }

        public Builder sheet(SheetEntity sheet) {
            this.sheet = sheet;
            return this;
        }

        public RowEntity build() {
            return new RowEntity(dataJson, sheet);
        }
    }
    
    // equals, hashCode, toString...
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowEntity rowEntity = (RowEntity) o;
        return Objects.equals(id, rowEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}