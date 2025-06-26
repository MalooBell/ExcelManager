// CHEMIN : project/src/models/sheet-mapping.model.ts

/**
 * NOUVEAU : Interface décrivant une seule règle de mapping.
 * 'source' est le nom de la colonne dans le fichier Excel.
 * 'destination' est le nom du champ que nous voulons dans notre base de données.
 */
export interface MappingRule {
  source: string;
  destination: string;
}

/**
 * NOUVEAU : Interface représentant l'objet de configuration de mapping complet pour une feuille.
 * C'est l'équivalent TypeScript de notre DTO Java.
 */
export interface SheetMapping {
  mappings: MappingRule[];
  ignoreUnmapped: boolean;
}