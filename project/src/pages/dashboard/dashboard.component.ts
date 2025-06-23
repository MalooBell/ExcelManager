import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FileUploadComponent } from '../../components/file-upload/file-upload.component';
import { FileListComponent } from '../../components/file-list/file-list.component';
import { HistoryDetailModalComponent } from '../../components/history-detail-modal/history-detail-modal.component';
import { RowService } from '../../services/row.service';
import { ModificationHistory } from '../../models/row.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FileUploadComponent, FileListComponent, HistoryDetailModalComponent],
  template: `
    <div class="container py-8 page-transition">
      <div class="mb-8 slide-up">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">
          Gestionnaire de fichiers Excel
        </h1>
        <p class="text-gray-600">
          Téléchargez, traitez et analysez vos fichiers Excel avec des outils avancés
        </p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Upload Section -->
        <div class="lg:col-span-2 slide-up" style="animation-delay: 0.1s;">
          <div class="mb-8">
            <h2 class="text-xl font-semibold mb-4">Télécharger un fichier Excel</h2>
            <app-file-upload (uploadSuccess)="onUploadSuccess()"></app-file-upload>
          </div>

          <!-- Files List -->
          <div class="file-list-container">
            <app-file-list #fileList></app-file-list>
          </div>
        </div>

        <!-- History Section -->
        <div class="lg:col-span-1 slide-up" style="animation-delay: 0.2s;">
          <div class="card h-full flex flex-col">
            <div class="card-header">
              <h3 class="card-title">Historique des modifications</h3>
            </div>

            <div class="history-list-container">
              <div *ngIf="loadingHistory" class="text-center py-8">
                <div class="loading-spinner"></div>
                <p class="text-gray-600 mt-2">Chargement...</p>
              </div>

              <div *ngIf="!loadingHistory && history.length === 0" class="text-center py-8">
                <div class="mb-4">
                  <svg class="w-12 h-12 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                </div>
                <p class="text-gray-500">Aucune modification récente</p>
              </div>

              <div *ngIf="!loadingHistory && history.length > 0" class="space-y-3">
                <div *ngFor="let item of history.slice(0, 10)" 
                     class="history-item"
                     [class.create]="item.operationType === 'CREATE'"
                     [class.update]="item.operationType === 'UPDATE'"
                     [class.delete]="item.operationType === 'DELETE'"
                     (click)="showHistoryDetails(item)">
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
                    <strong>{{ item.sheetName || 'Feuille inconnue' }}</strong>
                  </p>
                  <p class="text-xs text-gray-500">
                    Ligne ID: {{ item.rowEntityId }}
                  </p>
                  <div class="flex items-center justify-between mt-2">
                    <span class="text-xs text-gray-400">Cliquez pour voir les détails</span>
                    <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            <div *ngIf="history.length > 10" class="text-center pt-4 border-t mt-auto">
              <button class="btn btn-outline btn-sm" (click)="loadAllHistory()">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                </svg>
                Voir tout l'historique
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- History Detail Modal -->
    <app-history-detail-modal 
      *ngIf="selectedHistoryItem"
      [historyItem]="selectedHistoryItem"
      (closeModal)="closeHistoryDetails()">
    </app-history-detail-modal>
  `,
  styles: [`
    .file-list-container {
      max-height: 500px;
      overflow-y: auto;
      border: 1px solid var(--gray-200);
      border-radius: var(--border-radius-lg);
    }

    .history-list-container {
      flex-grow: 1;
      overflow-y: auto;
      padding-right: 0.5rem;
    }

    .h-full {
      height: 100%;
    }
    .flex-col {
      flex-direction: column;
    }
    .mt-auto {
      margin-top: auto;
    }
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
    .pt-4 {
      padding-top: 1rem;
    }
    .border-t {
      border-top: 1px solid var(--gray-200);
    }
    .text-xs { font-size: 0.75rem; }
    .w-4 { width: 1rem; }
    .h-4 { height: 1rem; }
    .w-12 { width: 3rem; }
    .h-12 { height: 3rem; }
    .mx-auto { margin-left: auto; margin-right: auto; }

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
  selectedHistoryItem: ModificationHistory | null = null;

  constructor(
    private rowService: RowService,
    private router: Router
  ) {}

  @ViewChild(FileListComponent) fileList!: FileListComponent;

  ngOnInit() {
    this.loadHistory();
  }

  onUploadSuccess() {
    if (this.fileList) {
      this.fileList.loadFiles();
    }
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

  showHistoryDetails(historyItem: ModificationHistory) {
    this.selectedHistoryItem = historyItem;
  }

  closeHistoryDetails() {
    this.selectedHistoryItem = null;
  }

  loadAllHistory() {
    this.router.navigate(['/history']);
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