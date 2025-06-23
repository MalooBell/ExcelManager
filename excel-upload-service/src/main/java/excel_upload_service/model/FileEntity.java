// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/model/FileEntity.java
package excel_upload_service.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    // La relation est maintenant avec les feuilles, pas directement avec les lignes.
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference // Permet la sérialisation des feuilles depuis le fichier
    private List<SheetEntity> sheets;

    @PrePersist
    protected void onCreate() {
        uploadTimestamp = LocalDateTime.now();
    }

    // --- Constructeurs, Getters, et Setters ---
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
    
    // Nouveaux getters/setters pour les feuilles
    public List<SheetEntity> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetEntity> sheets) {
        this.sheets = sheets;
    }
}