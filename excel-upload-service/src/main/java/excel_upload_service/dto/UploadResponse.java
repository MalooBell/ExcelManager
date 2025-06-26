// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/UploadResponse.java
package excel_upload_service.dto;

import java.util.List;

public class UploadResponse {
    private boolean success;
    private String message;
    private List<String> errors;
    
    // NOUVEAU : Champs pour piloter le frontend
    private Long fileId; // L'ID du fichier créé
    private boolean needsManualValidation; // `true` si l'utilisateur doit intervenir

    // Constructeur pour le succès
    public UploadResponse(boolean success, String message, Long fileId, boolean needsManualValidation) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
        this.needsManualValidation = needsManualValidation;
    }

    // Constructeur pour l'échec
    public UploadResponse(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = errors;
    }
    
    // --- Getters et Setters ---
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public boolean isNeedsManualValidation() { return needsManualValidation; }
    public void setNeedsManualValidation(boolean needsManualValidation) { this.needsManualValidation = needsManualValidation; }
}