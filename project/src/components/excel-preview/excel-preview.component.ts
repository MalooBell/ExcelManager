// CHEMIN : project/src/components/sheet-preview-modal/sheet-preview-modal.component.ts

import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExcelPreviewService, SheetPreview } from '../../services/excel-preview.service'; // Import du nouveau service

/**
 * NOUVEAU : Composant pour la modale d'aperçu et de sélection de l'en-tête.
 */
@Component({
  selector: 'app-sheet-preview-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" (click)="close()">
      <div class="modal" style="width: 95vw; max-width: 1400px;" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Sélectionnez la Ligne d'En-tête</h3>
          <button class="modal-close" (click)="close()">×</button>
        </div>
        <div class="modal-body" style="max-height: calc(90vh - 150px); overflow-y: auto;">
          <p class="mb-4 text-center text-gray-600">
            Cliquez sur le **numéro de la ligne** qui contient les en-têtes de votre tableau.
          </p>

          <div *ngIf="loading" class="text-center py-8">
              <div class="loading-spinner" style="width: 3rem; height: 3rem;"></div>
          </div>

          <div *ngIf="!loading && preview" class="overflow-x-auto border rounded-lg">
            <table class="table-preview">
              <tbody>
                <tr *ngFor="let row of preview.previewRows; let i = index" 
                    [class.selected-header]="(i + 1) === selectedRowIndex"
                    (mouseover)="hoveredRowIndex = i + 1"
                    (mouseleave)="hoveredRowIndex = null">
                  <td class="row-selector" (click)="selectHeader(i + 1)">
                    <span *ngIf="hoveredRowIndex === i + 1">→</span>
                    {{ i + 1 }}
                  </td>
                  <td *ngFor="let cell of row">{{ cell }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .table-preview { width: 100%; border-collapse: collapse; table-layout: fixed; }
    .table-preview td { 
      padding: 8px 12px; 
      border: 1px solid #e5e7eb; 
      font-size: 0.8rem; 
      white-space: nowrap; 
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .row-selector { 
      background-color: #f3f4f6; 
      font-weight: bold; 
      cursor: pointer; 
      text-align: center;
      position: sticky;
      left: 0;
      z-index: 1;
      width: 60px;
      transition: background-color 0.2s, color 0.2s;
    }
    tr:hover .row-selector { background-color: #3b82f6; color: white; }
    .selected-header, tr.selected-header .row-selector { background-color: #dbeafe !important; font-weight: bold; }
  `]
})
export class SheetPreviewModalComponent implements OnInit {
  // Entrées (Inputs) : Données fournies par le composant parent
  @Input() fileId!: number;
  @Input() sheetId!: number;
  @Input() sheetIndex!: number;

  // Sorties (Outputs) : Événements émis vers le composant parent
  @Output() headerSelected = new EventEmitter<number>();
  @Output() closeModal = new EventEmitter<void>();

  loading = true;
  preview: SheetPreview | null = null;
  selectedRowIndex: number | null = null;
  hoveredRowIndex: number | null = null;

  constructor(private previewService: ExcelPreviewService) {}

  ngOnInit() {
    this.loading = true;
    this.previewService.getSheetPreview(this.fileId, this.sheetIndex).subscribe({
      next: data => {
        this.preview = data;
        this.loading = false;
      },
      error: err => {
        console.error("Failed to load sheet preview", err);
        this.loading = false;
        alert("Erreur lors du chargement de l'aperçu du fichier.");
        this.close();
      }
    });
  }

  /**
   * Appelé lorsque l'utilisateur clique sur un numéro de ligne.
   * @param rowIndex Le numéro de la ligne sélectionnée (basé sur 1).
   */
  selectHeader(rowIndex: number) {
    this.selectedRowIndex = rowIndex;
    // Émet l'événement vers le composant parent avec le numéro de la ligne choisie.
    this.headerSelected.emit(rowIndex);
    // Ferme la modale automatiquement après la sélection.
    this.close();
  }

  /**
   * Émet l'événement pour fermer la modale.
   */
  close() {
    this.closeModal.emit();
  }
}