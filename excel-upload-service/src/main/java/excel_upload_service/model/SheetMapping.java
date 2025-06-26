// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/model/SheetMapping.java
package excel_upload_service.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NOUVEAU : Cette entité représente les règles de mapping pour une feuille Excel spécifique.
 * Elle permet de définir comment les colonnes du fichier source doivent être transformées
 * avant d'être sauvegardées dans le champ `dataJson` de l'entité RowEntity.
 */
@Entity
@Table(name = "sheet_mappings")
public class SheetMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chaque mapping est unique pour une feuille (SheetEntity).
     * La relation est OneToOne, car une feuille ne peut avoir qu'un seul jeu de règles de mapping.
     * `fetch = FetchType.LAZY` est utilisé pour ne charger le mapping que lorsque c'est nécessaire.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false, unique = true)
    @JsonBackReference // Empêche les boucles de sérialisation lors de la conversion en JSON.
    private SheetEntity sheet;

    /**
     * Ce champ stocke la définition du mapping au format JSON.
     * Utiliser un champ JSON nous donne une flexibilité maximale pour définir des règles complexes
     * sans avoir à modifier la structure de la base de données.
     * Exemple de contenu JSON :
     * {
     * "mappings": [
     * { "source": "Nom de l'employé", "destination": "employeeName", "type": "string" },
     * { "source": "Date Embauche", "destination": "hireDate", "type": "date", "format": "dd/MM/yyyy" }
     * ],
     * "ignoreUnmapped": true,
     * "allowNewColumns": false
     * }
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

    public SheetMapping() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SheetEntity getSheet() {
        return sheet;
    }

    public void setSheet(SheetEntity sheet) {
        this.sheet = sheet;
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