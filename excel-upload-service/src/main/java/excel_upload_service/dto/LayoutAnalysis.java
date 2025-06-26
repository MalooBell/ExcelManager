// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/LayoutAnalysis.java
package excel_upload_service.dto;

/**
 * NOUVEAU : Cette classe sert à stocker les résultats de l'analyse de la structure d'une feuille Excel.
 * Elle nous permet de transporter de manière propre l'index de la ligne d'en-tête et
 * l'index de la première ligne de données.
 */
public class LayoutAnalysis {

    private final int headerRowIndex;
    private final int dataStartRowIndex;
    private final boolean reliable; // Indique si la détection est considérée comme fiable

    public LayoutAnalysis(int headerRowIndex, boolean reliable) {
        this.headerRowIndex = headerRowIndex;
        // Les données commencent toujours sur la ligne qui suit l'en-tête.
        this.dataStartRowIndex = headerRowIndex + 1;
        this.reliable = reliable;
    }

    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    public int getDataStartRowIndex() {
        return dataStartRowIndex;
    }

    public boolean isReliable() {
        return reliable;
    }

    // Un constructeur statique pour le cas où aucune structure n'est trouvée.
    public static LayoutAnalysis unreliableDefault() {
        return new LayoutAnalysis(1, false);
    }
}