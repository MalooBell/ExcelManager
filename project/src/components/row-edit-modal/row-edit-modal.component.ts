// CHEMIN : project/src/components/row-edit-modal/row-edit-modal.component.ts
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
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div *ngFor="let column of columns; trackBy: trackByColumn" class="form-group">
                <label class="form-label">{{ column }}</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editableRow.data![column]"
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
  @Output() saveRow = new EventEmitter<Partial<RowEntity>>(); // MODIFIÉ pour accepter Partial<RowEntity>

  // MODIFIÉ : Définition de editableRow sans `sheetIndex`
  editableRow: Partial<RowEntity> = {
    id: undefined,
    data: {}
  };

  saving = false;

  ngOnInit() {
    if (this.row) {
      // MODIFIÉ : Initialisation sans `sheetIndex`
      this.editableRow = {
        id: this.row.id,
        data: { ...this.row.data }
      };
    } else {
      // MODIFIÉ : Initialisation sans `sheetIndex`
      this.editableRow = {
        id: undefined,
        data: {}
      };
      this.columns.forEach(column => {
        if (this.editableRow.data) {
          this.editableRow.data[column] = '';
        }
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

  // MODIFIÉ : La validation ne se base plus sur `sheetIndex`
  isFormValid(): boolean {
    return true; // Ou toute autre validation que vous jugerez nécessaire
  }

  trackByColumn(index: number, column: string): string {
    return column;
  }
}