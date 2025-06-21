package excel_upload_service.dto;


import java.util.List;

public class UploadResponse {
    private boolean success;
    private String message;
    private List<String> errors;
    private int processedRows;

    public UploadResponse() {}

    public UploadResponse(boolean success, String message, List<String> errors, int processedRows) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.processedRows = processedRows;
    }

    // Getters et Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public int getProcessedRows() { return processedRows; }
    public void setProcessedRows(int processedRows) { this.processedRows = processedRows; }
}
