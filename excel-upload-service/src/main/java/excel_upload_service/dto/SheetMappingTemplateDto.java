// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/dto/SheetMappingTemplateDto.java
package excel_upload_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SheetMappingTemplateDto {

    private Long id;
    private String name;
    private String description;

    @JsonRawValue
    private String mappingDefinitionJson;

    public SheetMappingTemplateDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMappingDefinitionJson() {
        return mappingDefinitionJson;
    }

    /**
     * MODIFIÉ : Renommé en 'setMappingDefinitionJson' pour la cohérence.
     * Jackson l'utilisera pour désérialiser le champ 'mappingDefinitionJson' du corps de la requête.
     * @param node L'objet JSON brut provenant de la requête.
     * @throws JsonProcessingException
     */
    @JsonProperty("mappingDefinition")
    public void setMappingDefinitionJson(JsonNode node) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        this.mappingDefinitionJson = mapper.writeValueAsString(node);
    }
    
    // NOUVEAU : Ajout d'un setter direct pour la chaîne de caractères JSON.
    // Cela résout l'erreur de compilation dans le service.
    public void setMappingDefinitionJson(String json) {
        this.mappingDefinitionJson = json;
    }
}