import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModificationHistory } from '../../models/row.model';

@Component({
  selector: 'app-history-detail-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal" style="width: 90vw; max-width: 800px;">
        <div class="modal-header">
          <h3 class="modal-title">
            Détails de la modification
          </h3>
          <button class="modal-close" (click)="close()">×</button>
        </div>

        <div class="modal-body">
          <div class="space-y-4">
            <div class="card">
              <div class="flex items-center justify-between mb-4">
                <span class="badge" 
                      [class.badge-success]="historyItem.operationType === 'CREATE'"
                      [class.badge-info]="historyItem.operationType === 'UPDATE'"
                      [class.badge-error]="historyItem.operationType === 'DELETE'">
                  {{ getOperationLabel(historyItem.operationType) }}
                </span>
                <span class="text-sm text-gray-500">
                  {{ formatDate(historyItem.timestamp) }}
                </span>
              </div>

              <div class="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label class="form-label">Feuille</label>
                  <p class="text-gray-900 font-medium">{{ historyItem.sheetName || 'Feuille inconnue' }}</p>
                </div>
                <div>
                  <label class="form-label">Ligne ID</label>
                  <p class="text-gray-900 font-medium">{{ historyItem.rowEntityId }}</p>
                </div>
              </div>

              <div *ngIf="historyItem.operationType === 'CREATE'" class="space-y-3">
                <h4 class="text-lg font-semibold text-gray-900">Données créées</h4>
                <div class="bg-green-50 border border-green-200 rounded-lg p-4">
                  <pre class="text-sm text-green-800 whitespace-pre-wrap">{{ formatJson(historyItem.newData) }}</pre>
                </div>
              </div>

              <div *ngIf="historyItem.operationType === 'DELETE'" class="space-y-3">
                <h4 class="text-lg font-semibold text-gray-900">Données supprimées</h4>
                <div class="bg-red-50 border border-red-200 rounded-lg p-4">
                  <pre class="text-sm text-red-800 whitespace-pre-wrap">{{ formatJson(historyItem.oldData) }}</pre>
                </div>
              </div>

              <div *ngIf="historyItem.operationType === 'UPDATE'" class="space-y-4">
                <h4 class="text-lg font-semibold text-gray-900">Comparaison des modifications</h4>
                
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <h5 class="font-medium text-gray-700 mb-2">Avant</h5>
                    <div class="bg-red-50 border border-red-200 rounded-lg p-4">
                      <pre class="text-sm text-red-800 whitespace-pre-wrap">{{ formatJson(historyItem.oldData) }}</pre>
                    </div>
                  </div>
                  <div>
                    <h5 class="font-medium text-gray-700 mb-2">Après</h5>
                    <div class="bg-green-50 border border-green-200 rounded-lg p-4">
                      <pre class="text-sm text-green-800 whitespace-pre-wrap">{{ formatJson(historyItem.newData) }}</pre>
                    </div>
                  </div>
                </div>

                <div class="mt-4">
                  <h5 class="font-medium text-gray-700 mb-2">Changements détectés</h5>
                  <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <div class="space-y-2">
                      <div *ngFor="let change of getChanges()" class="text-sm">
                        <span class="font-medium text-blue-800">{{ change.field }}:</span>
                        <span class="text-red-600 line-through ml-2">{{ change.oldValue }}</span>
                        <span class="text-green-600 ml-2">→ {{ change.newValue }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" (click)="close()">
            Fermer
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .space-y-3 > * + * {
      margin-top: 0.75rem;
    }
    .space-y-4 > * + * {
      margin-top: 1rem;
    }
    .space-y-2 > * + * {
      margin-top: 0.5rem;
    }
    .grid {
      display: grid;
    }
    .grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .grid-cols-2 {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .gap-4 {
      gap: 1rem;
    }
    .mb-2 { margin-bottom: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .ml-2 { margin-left: 0.5rem; }
    .bg-green-50 { background-color: #f0fdf4; }
    .bg-red-50 { background-color: #fef2f2; }
    .bg-blue-50 { background-color: #eff6ff; }
    .border-green-200 { border-color: #bbf7d0; }
    .border-red-200 { border-color: #fecaca; }
    .border-blue-200 { border-color: #bfdbfe; }
    .text-green-800 { color: #166534; }
    .text-red-800 { color: #991b1b; }
    .text-blue-800 { color: #1e40af; }
    .text-red-600 { color: #dc2626; }
    .text-green-600 { color: #16a34a; }
    .whitespace-pre-wrap { white-space: pre-wrap; }
    .line-through { text-decoration: line-through; }

    @media (min-width: 768px) {
      .md\\:grid-cols-2 {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
  `]
})
export class HistoryDetailModalComponent {
  @Input() historyItem!: ModificationHistory;
  @Output() closeModal = new EventEmitter<void>();

  onOverlayClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  close() {
    this.closeModal.emit();
  }

  getOperationLabel(operation: string): string {
    switch (operation) {
      case 'CREATE': return 'Création';
      case 'UPDATE': return 'Modification';
      case 'DELETE': return 'Suppression';
      default: return operation;
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatJson(jsonString: string | null): string {
    if (!jsonString) return '(vide)';
    try {
      const parsed = JSON.parse(jsonString);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return jsonString;
    }
  }

  getChanges(): Array<{field: string, oldValue: any, newValue: any}> {
    if (this.historyItem.operationType !== 'UPDATE' || !this.historyItem.oldData || !this.historyItem.newData) {
      return [];
    }

    try {
      const oldData = JSON.parse(this.historyItem.oldData);
      const newData = JSON.parse(this.historyItem.newData);
      const changes: Array<{field: string, oldValue: any, newValue: any}> = [];

      const allKeys = new Set([...Object.keys(oldData), ...Object.keys(newData)]);
      
      allKeys.forEach(key => {
        const oldValue = oldData[key];
        const newValue = newData[key];
        
        if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
          changes.push({
            field: key,
            oldValue: oldValue ?? '(vide)',
            newValue: newValue ?? '(vide)'
          });
        }
      });

      return changes;
    } catch {
      return [];
    }
  }
}