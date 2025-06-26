// CHEMIN : project/src/models/excel-preview.model.ts

/**
 * NOUVEAU : Définit la structure de la réponse de l'API d'aperçu.
 * C'est un objet simple contenant une seule propriété : un tableau de tableaux de chaînes de caractères,
 * représentant la grille des premières lignes du fichier Excel.
 */
export interface SheetPreview {
  previewRows: string[][];
}