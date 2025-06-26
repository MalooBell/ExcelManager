// CHEMIN : project/src/services/sheet-mapping.service.ts
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from './api.service';
import { SheetMapping } from '../models/sheet-mapping.model';

/**
 * NOUVEAU : Service dédié à la gestion des configurations de mapping.
 */
@Injectable({
  providedIn: 'root'
})
export class SheetMappingService {

  constructor(private api: ApiService) {}

  /**
   * Récupère le mapping pour une feuille spécifique.
   * @param sheetId L'ID de la feuille.
   * @returns Un Observable contenant le SheetMapping ou null si non trouvé.
   */
  getMapping(sheetId: number): Observable<SheetMapping | null> {
    return this.api.get<SheetMapping>(`/sheets/${sheetId}/mapping`).pipe(
      catchError(error => {
        // Si l'API retourne 404 (Not Found), on traite cela comme une absence de mapping (null).
        if (error.status === 404) {
          return of(null);
        }
        // Pour les autres erreurs, on les propage.
        throw error;
      })
    );
  }

  /**
   * Sauvegarde la configuration de mapping pour une feuille.
   * @param sheetId L'ID de la feuille.
   * @param mapping La configuration de mapping à sauvegarder.
   * @returns Un Observable avec le mapping sauvegardé.
   */
  saveMapping(sheetId: number, mapping: SheetMapping): Observable<SheetMapping> {
    return this.api.post<SheetMapping>(`/sheets/${sheetId}/mapping`, mapping);
  }

  /**
   * NOUVEAU : Appelle l'API pour appliquer un modèle à une feuille.
   * @param sheetId L'ID de la feuille à modifier.
   * @param templateId L'ID du modèle à appliquer.
   * @returns Un Observable contenant le nouveau mapping de la feuille.
   */
  applyTemplate(sheetId: number, templateId: number): Observable<SheetMapping> {
    return this.api.post<SheetMapping>(`/sheets/${sheetId}/mapping/apply-template/${templateId}`, {});
  }
}