// CHEMIN : project/src/services/excel-preview.service.ts

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { SheetPreview } from '../models/excel-preview.model'; // Import de notre nouveau modèle

/**
 * NOUVEAU : Service entièrement dédié à la prévisualisation et au retraitement des feuilles.
 * Il centralise la logique de communication avec les API spécifiques à cette fonctionnalité.
 */
@Injectable({
  providedIn: 'root'
})
export class ExcelPreviewService {

  constructor(private api: ApiService) {}

  /**
   * Récupère un aperçu des premières lignes d'une feuille.
   * @param fileId L'ID du fichier parent.
   * @param sheetIndex L'index de la feuille (0, 1, 2...).
   * @param limit Le nombre de lignes à récupérer pour l'aperçu.
   * @returns Un Observable contenant l'aperçu de la feuille.
   */
  getSheetPreview(fileId: number, sheetIndex: number, limit: number = 25): Observable<SheetPreview> {
    return this.api.get<SheetPreview>(`/preview/file/${fileId}/sheet/${sheetIndex}?limit=${limit}`);
  }

  /**
   * Envoie une requête au backend pour relancer le traitement d'une feuille avec une ligne d'en-tête spécifiée.
   * @param sheetId L'ID de la feuille à retraiter.
   * @param headerRowIndex Le numéro de la ligne (basé sur 1) que l'utilisateur a sélectionné comme en-tête.
   * @returns Un Observable vide qui se complète lorsque l'opération est terminée.
   */
  reprocessSheet(sheetId: number, headerRowIndex: number): Observable<void> {
    // Le corps de la requête contient l'index de l'en-tête.
    const payload = { headerRowIndex };
    return this.api.post<void>(`/excel/sheet/${sheetId}/reprocess`, payload);
  }
}

export { SheetPreview };
