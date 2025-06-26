// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/LayoutAnalysis.java
package excel_upload_service.dto;

public class LayoutAnalysis {
    private final int headerRowIndex;
    private final boolean headerDetected;

    public LayoutAnalysis(int headerRowIndex, boolean headerDetected) {
        this.headerRowIndex = headerRowIndex;
        this.headerDetected = headerDetected;
    }

    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    public boolean isHeaderDetected() {
        return headerDetected;
    }
}