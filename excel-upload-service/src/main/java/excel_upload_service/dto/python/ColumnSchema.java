// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/dto/python/ColumnSchema.java
package excel_upload_service.dto.python;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignore les propriétés inconnues pour plus de flexibilité si le modèle Python évolue
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColumnSchema {
    private String name;
    private String type;

    // Getters et Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}