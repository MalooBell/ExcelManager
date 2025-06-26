export interface RowEntity {
  id: number;
  // MODIFIÉ : La propriété `sheetIndex` a été supprimée car elle n'appartient plus à la ligne.
  // Elle est maintenant une propriété de l'entité Sheet.

  /**
   * MODIFIÉ : Utilisation de `Record<string, any>` pour définir explicitement un dictionnaire
   * avec des clés de type string et des valeurs de n'importe quel type.
   * Cela résout les erreurs d'inférence de type et indique clairement notre intention au compilateur.
   */
  data: Record<string, any>;
  sheetName?: string;
}

export interface ModificationHistory {
  id: number;
  rowEntityId: number;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldData: string | null;
  newData: string | null;
  timestamp: string;
  sheetName?: string; // Ajout pour identifier par sheetname
}