// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/UploadResponse.java
package excel_upload_service.dto;

import java.util.List;

public class UploadResponse {
    private boolean success;
    private String message;
    private Long fileId;
    //private List errors; // Nullable, only present on successful uploads

    public UploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // NEW CONSTRUCTOR TO MATCH USAGE IN EXCELUPLOADSERVICEIMPL
    public UploadResponse(boolean success, String message, Long fileId) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
    }

//     public UploadResponse(boolean success, String message, java.util.List<String> errors) {
//     this.success = success;
//     this.message = message;
//     this.errors = errors;
// }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}