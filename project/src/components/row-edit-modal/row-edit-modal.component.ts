import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RowEntity } from '../../models/row.model';

@Component({
  selector: 'app-row-edit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal" style="width: 90vw; max-width: 800px;">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ isEditMode ? 'Modifier la ligne' : 'Ajouter une nouvelle ligne' }}
          </h3>
          <button class="modal-close" (click)="close()">×</button>
        </div>

        <div class="modal-body">
          <form (ngSubmit)="save()" #form="ngForm">
            <div class="form-group">
              <label class="form-label">Index de la feuille</label>
              <input
                type="number"
                class="form-control"
                [(ngModel)]="editableRow.sheetIndex"
                name="sheetIndex"
                required
                min="0">
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div *ngFor="let column of columns; trackBy: trackByColumn" class="form-group">
                <label class="form-label">{{ column }}</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editableRow.data[column]"
                  [name]="column"
                  placeholder="Entrez une valeur">
              </div>
            </div>

            <div *ngIf="columns.length === 0" class="text-center py-8">
              <p class="text-gray-500">Aucune colonne détectée. Vérifiez que le fichier contient des données.</p>
            </div>
          </form>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" (click)="close()">
            Annuler
          </button>
          <button type="button" class="btn btn-primary" (click)="save()" [disabled]="!isFormValid()">
            <div *ngIf="saving" class="loading-spinner"></div>
            {{ isEditMode ? 'Modifier' : 'Créer' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grid {
      display: grid;
    }
    .grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .gap-4 {
      gap: var(--spacing-4);
    }
    .max-h-80 {
      max-height: 20rem;
    }
    .overflow-y-auto {
      overflow-y: auto;
    }

    @media (min-width: 768px) {
      .md\\:grid-cols-2 {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
  `]
})
export class RowEditModalComponent implements OnInit {
  @Input() row: RowEntity | null = null;
  @Input() columns: string[] = [];
  @Input() isEditMode = false;
  @Output() closeModal = new EventEmitter<void>();
  @Output() saveRow = new EventEmitter<RowEntity>();

  editableRow: RowEntity = {
    id: 0,
    sheetIndex: 0,
    data: {}
  };

  saving = false;

  ngOnInit() {
    if (this.row) {
      this.editableRow = {
        id: this.row.id,
        sheetIndex: this.row.sheetIndex,
        data: { ...this.row.data }
      };
    } else {
      // Initialize with empty data for all columns
      this.editableRow = {
        id: 0,
        sheetIndex: 0,
        data: {}
      };
      this.columns.forEach(column => {
        this.editableRow.data[column] = '';
      });
    }
  }

  onOverlayClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  close() {
    this.closeModal.emit();
  }

  save() {
    if (!this.isFormValid()) {
      return;
    }

    this.saving = true;
    this.saveRow.emit(this.editableRow);
  }

  isFormValid(): boolean {
    return this.editableRow.sheetIndex >= 0;
  }

  trackByColumn(index: number, column: string): string {
    return column;
  }
}