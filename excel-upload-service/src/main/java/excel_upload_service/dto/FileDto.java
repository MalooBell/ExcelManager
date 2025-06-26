// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/FileDto.java
package excel_upload_service.dto;

import java.time.LocalDateTime;

/**
 * NOUVEAU : DTO pour représenter un fichier dans une liste.
 * Ne contient que les informations nécessaires à l'affichage,
 * évitant les problèmes de sérialisation d'objets complexes.
 */
public class FileDto {
    private Long id;
    private String fileName;
    private LocalDateTime uploadTimestamp;
    private int sheetCount; // On ne veut que le *nombre* de feuilles, pas les feuilles elles-mêmes.

    // --- Constructeurs, Getters et Setters ---

    public FileDto() {}

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

    public int getSheetCount() {
        return sheetCount;
    }

    public void setSheetCount(int sheetCount) {
        this.sheetCount = sheetCount;
    }
}