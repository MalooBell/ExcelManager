package excel_upload_service.dto;



import java.util.List;

/**
 * NOUVEAU : DTO pour transporter un aperçu des données d'une feuille.
 * Il contient une liste de lignes, où chaque ligne est une liste de chaînes de caractères.
 */
public class SheetPreviewDto {

    private List<List<String>> previewRows;

    public SheetPreviewDto(List<List<String>> previewRows) {
        this.previewRows = previewRows;
    }

    public List<List<String>> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<List<String>> previewRows) {
        this.previewRows = previewRows;
    }

    
}
