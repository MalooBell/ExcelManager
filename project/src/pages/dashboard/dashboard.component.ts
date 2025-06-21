import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadComponent } from '../../components/file-upload/file-upload.component';
import { FileListComponent } from '../../components/file-list/file-list.component';
import { RowService } from '../../services/row.service';
import { ModificationHistory } from '../../models/row.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FileUploadComponent, FileListComponent],
  template: `
    <div class="container py-8">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">
          Gestionnaire de fichiers Excel
        </h1>
        <p class="text-gray-600">
          Téléchargez, traitez et analysez vos fichiers Excel avec des outils avancés
        </p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Upload Section -->
        <div class="lg:col-span-2">
          <div class="mb-8">
            <h2 class="text-xl font-semibold mb-4">Télécharger un fichier Excel</h2>
            <app-file-upload (uploadSuccess)="onUploadSuccess()"></app-file-upload>
          </div>

          <!-- Files List -->
          <div>
            <app-file-list #fileList></app-file-list>
          </div>
        </div>

        <!-- History Section -->
        <div class="lg:col-span-1">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">Historique des modifications</h3>
            </div>

            <div *ngIf="loadingHistory" class="text-center py-8">
              <div class="loading-spinner"></div>
              <p class="text-gray-600 mt-2">Chargement...</p>
            </div>

            <div *ngIf="!loadingHistory && history.length === 0" class="text-center py-8">
              <p class="text-gray-500">Aucune modification récente</p>
            </div>

            <div *ngIf="!loadingHistory && history.length > 0" class="space-y-3">
              <div *ngFor="let item of history.slice(0, 10)" class="border-l-4 pl-4 py-2"
                   [class.border-green-500]="item.operationType === 'CREATE'"
                   [class.border-blue-500]="item.operationType === 'UPDATE'"
                   [class.border-red-500]="item.operationType === 'DELETE'">
                <div class="flex items-center justify-between">
                  <span class="badge" 
                        [class.badge-success]="item.operationType === 'CREATE'"
                        [class.badge-info]="item.operationType === 'UPDATE'"
                        [class.badge-error]="item.operationType === 'DELETE'">
                    {{ getOperationLabel(item.operationType) }}
                  </span>
                  <span class="text-xs text-gray-500">
                    {{ formatDate(item.timestamp) }}
                  </span>
                </div>
                <p class="text-sm text-gray-600 mt-1">
                  Ligne ID: {{ item.rowEntityId }}
                </p>
              </div>
            </div>

            <div *ngIf="history.length > 10" class="text-center pt-4 border-t">
              <button class="btn btn-outline btn-sm" (click)="loadAllHistory()">
                Voir tout l'historique
              </button>
            </div>
          </div>
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
    .gap-8 {
      gap: 2rem;
    }
    .space-y-3 > * + * {
      margin-top: 0.75rem;
    }
    .border-l-4 {
      border-left-width: 4px;
    }
    .pl-4 {
      padding-left: 1rem;
    }
    .py-2 {
      padding-top: 0.5rem;
      padding-bottom: 0.5rem;
    }
    .pt-4 {
      padding-top: 1rem;
    }
    .border-t {
      border-top: 1px solid var(--gray-200);
    }
    .border-green-500 { border-color: #10b981; }
    .border-blue-500 { border-color: #3b82f6; }
    .border-red-500 { border-color: #ef4444; }
    .text-xs { font-size: 0.75rem; }

    @media (min-width: 1024px) {
      .lg\\:grid-cols-3 {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
      .lg\\:col-span-1 {
        grid-column: span 1 / span 1;
      }
      .lg\\:col-span-2 {
        grid-column: span 2 / span 2;
      }
    }
  `]
})
export class DashboardComponent implements OnInit {
  history: ModificationHistory[] = [];
  loadingHistory = true;

  constructor(private rowService: RowService) {}

  ngOnInit() {
    this.loadHistory();
  }

  onUploadSuccess() {
    // Refresh the file list
    const fileList = document.querySelector('app-file-list') as any;
    if (fileList && fileList.loadFiles) {
      fileList.loadFiles();
    }
    
    // Refresh history
    this.loadHistory();
  }

  loadHistory() {
    this.loadingHistory = true;
    this.rowService.getAllHistory().subscribe({
      next: (history) => {
        this.history = history;
        this.loadingHistory = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l\'historique:', error);
        this.loadingHistory = false;
      }
    });
  }

  loadAllHistory() {
    // Navigate to full history page or show modal
    console.log('Load all history - to be implemented');
  }

  getOperationLabel(operation: string): string {
    switch (operation) {
      case 'CREATE': return 'Créé';
      case 'UPDATE': return 'Modifié';
      case 'DELETE': return 'Supprimé';
      default: return operation;
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) {
      return 'À l\'instant';
    } else if (diffInMinutes < 60) {
      return `Il y a ${diffInMinutes} min`;
    } else if (diffInMinutes < 1440) {
      const hours = Math.floor(diffInMinutes / 60);
      return `Il y a ${hours}h`;
    } else {
      return date.toLocaleDateString('fr-FR');
    }
  }
}