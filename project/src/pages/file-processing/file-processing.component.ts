import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FileService } from '../../services/file.service';
import { RowService } from '../../services/row.service';
import { FileEntity, PageResponse } from '../../models/file.model';
import { RowEntity, ModificationHistory } from '../../models/row.model';
import { RowEditModalComponent } from '../../components/row-edit-modal/row-edit-modal.component';
import { GraphModalComponent } from '../../components/graph-modal/graph-modal.component';

@Component({
  selector: 'app-file-processing',
  standalone: true,
  imports: [CommonModule, FormsModule, RowEditModalComponent, GraphModalComponent],
  template: `
    <div class="container py-8" *ngIf="file">
      <!-- Header -->
      <div class="flex justify-between items-center mb-6">
        <div>
          <button (click)="goBack()" class="btn btn-secondary btn-sm mb-2">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
            </svg>
            Retour
          </button>
          <h1 class="text-2xl font-bold text-gray-900">{{ file.fileName }}</h1>
          <p class="text-gray-600">{{ file.totalRows }} lignes • Téléchargé le {{ formatDate(file.uploadTimestamp) }}</p>
        </div>
        <div class="flex gap-3">
          <button (click)="isShowingGraphModal = true" class="btn btn-success">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
            </svg>
            Graphique
          </button>
          <button (click)="downloadFile()" class="btn btn-primary">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            Télécharger
          </button>
        </div>
      </div>

      <!-- Search and Controls -->
     <div class="card mb-6">
  <div class="flex flex-col sm:flex-row gap-4 items-center">
    
    <div class="flex-1 w-full">
      <input
        type="text"
        class="form-control"
        placeholder="Rechercher dans les données..."
        [(ngModel)]="searchKeyword"
        (keyup.enter)="search()"
        (input)="onSearchInput($event)">
    </div>
    
    <div class="flex gap-2 items-center flex-wrap">
      
      <select id="row-page-size" class="form-control" [(ngModel)]="pageSize" (ngModelChange)="onPageSizeChange()">
        <option *ngFor="let size of pageSizes" [value]="size">{{ size }} lignes</option>
      </select>
      
      <select class="form-control" [(ngModel)]="sortField" (change)="onSortChange()">
        <option value="">Trier par...</option>
        <option *ngFor="let column of columns" [value]="'data.' + column">{{ column }}</option>
        <option value="sheetIndex">Index feuille</option>
      </select>
      
      <select class="form-control" [(ngModel)]="sortDirection" (change)="onSortChange()">
        <option value="asc">Croissant</option>
        <option value="desc">Décroissant</option>
      </select>
      
      <button (click)="showAddModal()" class="btn btn-success">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
        </svg>
        Ajouter
      </button>
    </div>

  </div>
</div>

      <!-- Loading -->
      <div *ngIf="loading" class="text-center py-8">
        <div class="loading-spinner"></div>
        <p class="text-gray-600 mt-2">Chargement des données...</p>
      </div>

      <!-- Data Table -->
      <div *ngIf="!loading" class="card">
        <div class="table-container">
        <div class="overflow-x-auto">
          <table class="table" *ngIf="rows.length > 0">
            <thead>
              <tr>
                <th>Feuille</th>
                <th *ngFor="let column of columns.slice(0, maxVisibleColumns)" 
                    [title]="column">
                  {{ column.length > 15 ? column.substring(0, 15) + '...' : column }}
                </th>
                <th *ngIf="columns.length > maxVisibleColumns">
                  +{{ columns.length - maxVisibleColumns }} colonnes
                </th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of rows" class="hover:bg-gray-50">
                <td>{{ row.sheetIndex }}</td>
                <td *ngFor="let column of columns.slice(0, maxVisibleColumns)" 
                    [title]="row.data[column]">
                  {{ truncateText(row.data[column], 20) }}
                </td>
                <td *ngIf="columns.length > maxVisibleColumns">
                  <button (click)="showRowDetails(row)" class="btn btn-outline btn-sm">
                    Voir tout
                  </button>
                </td>
                <td>
                  <div class="flex gap-1">
                    <button (click)="showEditModal(row)" class="btn btn-primary btn-sm" title="Modifier">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                      </svg>
                    </button>
                    <button (click)="showRowHistory(row.id)" class="btn btn-secondary btn-sm" title="Historique">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                    </button>
                    <button (click)="deleteRow(row.id)" class="btn btn-danger btn-sm" title="Supprimer">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div *ngIf="rows.length === 0" class="text-center py-8">
            <p class="text-gray-500">Aucune donnée trouvée</p>
          </div>
</div>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages > 1" class="pagination">
          <button class="pagination-btn" [class.disabled]="currentPage === 0" 
                  (click)="goToPage(0)" [disabled]="currentPage === 0">
            Première
          </button>
          <button class="pagination-btn" [class.disabled]="currentPage === 0" 
                  (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 0">
            Précédente
          </button>
          
          <div class="flex gap-1">
            <button *ngFor="let page of getVisiblePages()" 
                    class="pagination-btn"
                    [class.active]="page === currentPage"
                    (click)="goToPage(page)">
              {{ page + 1 }}
            </button>
          </div>
          
          <button class="pagination-btn" [class.disabled]="currentPage === totalPages - 1" 
                  (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages - 1">
            Suivante
          </button>
          <button class="pagination-btn" [class.disabled]="currentPage === totalPages - 1" 
                  (click)="goToPage(totalPages - 1)" [disabled]="currentPage === totalPages - 1">
            Dernière
          </button>
        </div>

        <div class="text-center text-sm text-gray-500 mt-4">
          Affichage de {{ (currentPage * pageSize) + 1 }} à {{ Math.min((currentPage + 1) * pageSize, totalElements) }} 
          sur {{ totalElements }} lignes
        </div>
      </div>
    </div>

    <!-- Edit Modal -->
    <app-row-edit-modal 
      *ngIf="isShowingEditModal"
      [row]="selectedRow"
      [columns]="columns"
      [isEditMode]="isEditMode"
      (closeModal)="closeEditModal()"
      (saveRow)="saveRow($event)">
    </app-row-edit-modal>

    <!-- Graph Modal -->
    <app-graph-modal 
  *ngIf="isShowingGraphModal"
  [fileId]="fileId"
  [columns]="columns"
  [sampleRows]="rows"  (closeModal)="isShowingGraphModal = false">
</app-graph-modal>

    <!-- Row History Modal -->
    <div *ngIf="isShowingHistoryModal" class="modal-overlay" (click)="closeHistoryModal()">
      <div class="modal" style="width: 90vw; max-width: 600px;">
        <div class="modal-header">
          <h3 class="modal-title">Historique de la ligne</h3>
          <button class="modal-close" (click)="closeHistoryModal()">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="loadingHistory" class="text-center py-4">
            <div class="loading-spinner"></div>
            <p class="text-gray-600 mt-2">Chargement de l'historique...</p>
          </div>
          
          <div *ngIf="!loadingHistory && rowHistory.length === 0" class="text-center py-4">
            <p class="text-gray-500">Aucun historique disponible</p>
          </div>
          
          <div *ngIf="!loadingHistory && rowHistory.length > 0" class="space-y-4">
            <div *ngFor="let history of rowHistory" class="border-l-4 pl-4 py-2"
                 [class.border-green-500]="history.operationType === 'CREATE'"
                 [class.border-blue-500]="history.operationType === 'UPDATE'"
                 [class.border-red-500]="history.operationType === 'DELETE'">
              <div class="flex justify-between items-center mb-2">
                <span class="badge"
                      [class.badge-success]="history.operationType === 'CREATE'"
                      [class.badge-info]="history.operationType === 'UPDATE'"
                      [class.badge-error]="history.operationType === 'DELETE'">
                  {{ getOperationLabel(history.operationType) }}
                </span>
                <span class="text-sm text-gray-500">
                  {{ formatDate(history.timestamp) }}
                </span>
              </div>
              <div *ngIf="history.operationType === 'UPDATE'" class="text-sm">
                <p class="text-gray-600 mb-1">Anciennes données:</p>
                <pre class="bg-gray-100 p-2 rounded text-xs">{{ formatJson(history.oldData) }}</pre>
                <p class="text-gray-600 mb-1 mt-2">Nouvelles données:</p>
                <pre class="bg-gray-100 p-2 rounded text-xs">{{ formatJson(history.newData) }}</pre>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="closeHistoryModal()">Fermer</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .w-3 { width: 0.75rem; }
    .h-3 { height: 0.75rem; }
    .w-4 { width: 1rem; }
    .h-4 { height: 1rem; }
    .flex-1 { flex: 1 1 0%; }
    .space-y-4 > * + * { margin-top: 1rem; }
    .overflow-x-auto { overflow-x: auto; }
    .hover\\:bg-gray-50:hover { background-color: var(--gray-50); }
    .border-l-4 { border-left-width: 4px; }
    .pl-4 { padding-left: 1rem; }
    .py-2 { padding-top: 0.5rem; padding-bottom: 0.5rem; }
    .border-green-500 { border-color: #10b981; }
    .border-blue-500 { border-color: #3b82f6; }
    .border-red-500 { border-color: #ef4444; }
    .bg-gray-100 { background-color: var(--gray-100); }
    .text-xs { font-size: 0.75rem; }
    
    @media (max-width: 640px) {
      .flex-col { flex-direction: column; }
      .sm\\:flex-row { flex-direction: row; }
    }

    pre {
      white-space: pre-wrap;
      word-break: break-word;
      max-height: 150px;
      overflow-y: auto;
    }
      .table-container {
  /* Définit une hauteur maximale relative à la hauteur de la fenêtre du navigateur (60% de la hauteur visible). */
  /* Vous pouvez aussi utiliser une valeur fixe comme 500px. */
  max-height: 60vh;
  
  /* Active le défilement vertical (scrollbar) si le contenu de la table dépasse la max-height. */
  overflow-y: auto;
}
  `]
})
export class FileProcessingComponent implements OnInit {
  fileId!: number;
  file: FileEntity | null = null;
  rows: RowEntity[] = [];
  columns: string[] = [];
  
  // Pagination
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 50; // Valeur par défaut
  
  // AJOUT : Options de taille de page disponibles
  pageSizes: number[] = [25, 50, 100, 200];
  // Search and sort
  searchKeyword = '';
  sortField = '';
  sortDirection = 'asc';
  
  // UI state
  loading = true;
  maxVisibleColumns = 5;
  
  // Modals - renamed to avoid conflicts
  isShowingEditModal = false;
  isShowingGraphModal = false;
  isShowingHistoryModal = false;
  selectedRow: RowEntity | null = null;
  isEditMode = false;
  rowHistory: ModificationHistory[] = [];
  loadingHistory = false;
  
  // Debounce timer
  private searchTimeout: any;

  // Make Math available in template
  Math = Math;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fileService: FileService,
    private rowService: RowService
  ) {}

  ngOnInit() {
    this.fileId = Number(this.route.snapshot.params['id']);
    this.loadFile();
    this.loadRows();
  }

  onPageSizeChange(): void {
    this.currentPage = 0; // Toujours revenir à la première page
    this.loadRows();
  }

  loadFile() {
    this.fileService.getFile(this.fileId).subscribe({
      next: (file) => {
        this.file = file;
        if (file.headersJson) {
          this.columns = JSON.parse(file.headersJson);
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement du fichier:', error);
        this.router.navigate(['/']);
      }
    });
  }

  loadRows() {
    this.loading = true;
    
    let sort = '';
    if (this.sortField) {
      sort = `${this.sortField},${this.sortDirection}`;
    }
    
    this.rowService.getRowsForFile(
      this.fileId,
      this.currentPage,
      this.pageSize,
      this.searchKeyword || undefined,
      sort || undefined
    ).subscribe({
      next: (response: PageResponse<RowEntity>) => {
        this.rows = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des lignes:', error);
        this.loading = false;
      }
    });
  }

  onSearchInput(event: any) {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.search();
    }, 500);
  }

  search() {
    this.currentPage = 0;
    this.loadRows();
  }

  onSortChange() {
    this.currentPage = 0;
    this.loadRows();
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadRows();
    }
  }

  getVisiblePages(): number[] {
    const delta = 2;
    const range = [];
    const rangeWithDots = [];
    
    for (let i = Math.max(2, this.currentPage - delta);
         i <= Math.min(this.totalPages - 2, this.currentPage + delta);
         i++) {
      range.push(i);
    }
    
    if (this.currentPage - delta > 2) {
      rangeWithDots.push(0, 1, -1);
    } else {
      rangeWithDots.push(0, 1);
    }
    
    rangeWithDots.push(...range);
    
    if (this.currentPage + delta < this.totalPages - 2) {
      rangeWithDots.push(-1, this.totalPages - 2, this.totalPages - 1);
    } else {
      rangeWithDots.push(this.totalPages - 2, this.totalPages - 1);
    }
    
    return rangeWithDots.filter((v, i, a) => a.indexOf(v) === i && v >= 0);
  }

  showAddModal() {
    this.selectedRow = null;
    this.isEditMode = false;
    this.isShowingEditModal = true;
  }

  showEditModal(row: RowEntity) {
    this.selectedRow = row;
    this.isEditMode = true;
    this.isShowingEditModal = true;
  }

  closeEditModal() {
    this.isShowingEditModal = false;
    this.selectedRow = null;
  }

  saveRow(row: RowEntity) {
    if (this.isEditMode && row.id) {
      this.rowService.updateRow(row.id, row).subscribe({
        next: () => {
          this.closeEditModal();
          this.loadRows();
        },
        error: (error) => {
          console.error('Erreur lors de la modification:', error);
        }
      });
    } else {
      this.rowService.createRow(this.fileId, row).subscribe({
        next: () => {
          this.closeEditModal();
          this.loadRows();
          this.loadFile(); // Refresh file stats
        },
        error: (error) => {
          console.error('Erreur lors de la création:', error);
        }
      });
    }
  }

  deleteRow(rowId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette ligne ?')) {
      this.rowService.deleteRow(rowId).subscribe({
        next: () => {
          this.loadRows();
          this.loadFile(); // Refresh file stats
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
        }
      });
    }
  }

  showRowHistory(rowId: number) {
    this.loadingHistory = true;
    this.isShowingHistoryModal = true;
    
    this.rowService.getRowHistory(rowId).subscribe({
      next: (history) => {
        this.rowHistory = history;
        this.loadingHistory = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l\'historique:', error);
        this.loadingHistory = false;
      }
    });
  }

  closeHistoryModal() {
    this.isShowingHistoryModal = false;
    this.rowHistory = [];
  }

  showRowDetails(row: RowEntity) {
    this.selectedRow = row;
    this.isEditMode = true;
    this.isShowingEditModal = true;
  }

  downloadFile() {
    if (this.file) {
      this.fileService.downloadFile(this.file.fileName, this.searchKeyword || undefined).subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = this.file!.fileName;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        },
        error: (error) => {
          console.error('Erreur lors du téléchargement:', error);
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/']);
  }

  truncateText(text: any, maxLength: number): string {
    if (!text) return '';
    const str = text.toString();
    return str.length > maxLength ? str.substring(0, maxLength) + '...' : str;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatJson(jsonString: string | null): string {
    if (!jsonString) return '';
    try {
      return JSON.stringify(JSON.parse(jsonString), null, 2);
    } catch {
      return jsonString;
    }
  }

  getOperationLabel(operation: string): string {
    switch (operation) {
      case 'CREATE': return 'Créé';
      case 'UPDATE': return 'Modifié';
      case 'DELETE': return 'Supprimé';
      default: return operation;
    }
  }
}