// CHEMIN : project/src/models/sheet-mapping-template.model.ts

/**
 * NOUVEAU : Interface décrivant un modèle de mapping, tel que reçu de l'API.
 */
export interface SheetMappingTemplate {
  id: number;
  name: string;
  description: string;
  // La définition du mapping est une chaîne JSON brute.
  mappingDefinitionJson: string;
}

/**
 * NOUVEAU : Interface pour la création d'un nouveau modèle. L'ID n'est pas nécessaire.
 */
export interface NewSheetMappingTemplate {
  name: string;
  description?: string;
  // On enverra un objet, qui sera converti en chaîne par le service.
  mappingDefinition: any;
}