package excel_upload_service.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NOUVEAU : Entité représentant un modèle de mapping réutilisable.
 * Ces modèles peuvent être créés par les utilisateurs pour être appliqués
 * rapidement à de nouvelles feuilles ayant une structure similaire.
 */
@Entity
@Table(name = "sheet_mapping_templates")
public class SheetMappingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Un nom unique et descriptif pour le modèle, défini par l'utilisateur.
     * Ex: "Mapping Rapport Ventes Mensuel", "Import RH Salariés"
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Une description optionnelle pour donner plus de contexte sur l'utilisation du modèle.
     */
    @Column(length = 512)
    private String description;

    /**
     * Le cœur du modèle : la définition JSON des règles de mapping.
     * Ce champ contient exactement le même type de JSON que celui que nous stockons
     * dans l'entité SheetMapping.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String mappingDefinitionJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Constructeurs, Getters, et Setters ---

    public SheetMappingTemplate() {
    }

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

    public void setMappingDefinitionJson(String mappingDefinitionJson) {
        this.mappingDefinitionJson = mappingDefinitionJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    } 
    
}
