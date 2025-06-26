// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/SheetMappingDto.java
package excel_upload_service.dto;

import java.util.List;
import java.util.Map;

/**
 * NOUVEAU : DTO pour transférer les données de mapping entre le frontend et le backend.
 * L'utilisation d'un DTO nous permet de découpler la représentation de l'API de l'entité de la base de données.
 */
public class SheetMappingDto {

    private List<Map<String, String>> mappings;
    private boolean ignoreUnmapped;

    // --- Constructeurs, Getters, et Setters ---

    public SheetMappingDto() {
    }

    public List<Map<String, String>> getMappings() {
        return mappings;
    }

    public void setMappings(List<Map<String, String>> mappings) {
        this.mappings = mappings;
    }

    public boolean isIgnoreUnmapped() {
        return ignoreUnmapped;
    }

    public void setIgnoreUnmapped(boolean ignoreUnmapped) {
        this.ignoreUnmapped = ignoreUnmapped;
    }
}