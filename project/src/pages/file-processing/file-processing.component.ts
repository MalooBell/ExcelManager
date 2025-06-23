import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FileService } from '../../services/file.service';
import { RowService } from '../../services/row.service';
import { FileEntity, PageResponse } from '../../models/file.model';
import { RowEntity, ModificationHistory } from '../../models/row.model';
import { SheetEntity } from '../../models/sheet.model';
import { RowEditModalComponent } from '../../components/row-edit-modal/row-edit-modal.component';
import { GraphModalComponent } from '../../components/graph-modal/graph-modal.component';

@Component({
  selector: 'app-file-processing',
  standalone: true,
  imports: [CommonModule, FormsModule, RowEditModalComponent, GraphModalComponent],
  template: `<div class="container py-8 page-transition" *ngIf="!loading && file">
    <div class="flex justify-between items-center mb-6 slide-up">
        <div>
            <button (click)="goBack()" class="btn btn-secondary btn-sm mb-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
              Retour
            </button>
            <h1 class="text-2xl font-bold text-gray-900">{{ file.fileName }}</h1>
            <p class="text-gray-600">Téléchargé le {{ formatDate(file.uploadTimestamp) }}</p>
        </div>
        <div class="btn-group" *ngIf="selectedSheet && activeView === 'data'">
            <button (click)="isShowingGraphModal = true" class="btn btn-success">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
              </svg>
              Graphique
            </button>
            <button (click)="downloadCurrentSheet()" class="btn btn-primary">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
              Télécharger
            </button>
        </div>
    </div>

    <div class="mb-6 border-b border-gray-200 slide-up" style="animation-delay: 0.1s;">
        <nav class="tab-nav" aria-label="Tabs">
            <button *ngFor="let sheet of sheets" (click)="selectSheet(sheet)"
                [ngClass]="{
                    'active': sheet.id === selectedSheet?.id && activeView === 'data'
                }" class="tab-btn">
                {{ sheet.sheetName }} ({{ sheet.totalRows }} lignes)
            </button>
            <button (click)="selectHistoryView()" *ngIf="selectedSheet"
                [ngClass]="{
                    'active': activeView === 'history'
                }" class="tab-btn">
                Historique de la feuille
            </button>
        </nav>
    </div>

    <div *ngIf="selectedSheet && activeView === 'data'" class="slide-up" style="animation-delay: 0.2s;">
        <div class="card mb-6">
            <div class="flex flex-col sm:flex-row gap-4 items-center">
                <div class="flex-1 w-full">
                    <input type="text" class="form-control" placeholder="Rechercher dans les données..."
                        [(ngModel)]="searchKeyword" (input)="onSearchInput()">
                </div>
                <div class="btn-group flex-wrap">
                    <select class="form-control" [(ngModel)]="pageSize" (ngModelChange)="onSortOrPageSizeChange()">
                        <option *ngFor="let size of pageSizes" [value]="size">{{ size }} lignes</option>
                    </select>
                    <select class="form-control" [(ngModel)]="sortField" (change)="onSortOrPageSizeChange()">
                        <option value="">Trier par...</option>
                        <option *ngFor="let column of columns" [value]="column">{{ column }}</option>
                    </select>
                    <select class="form-control" [(ngModel)]="sortDirection" (change)="onSortOrPageSizeChange()">
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

        <div *ngIf="loadingRows" class="text-center py-8">
            <div class="loading-spinner" style="width: 2rem; height: 2rem;"></div>
            <p class="text-gray-600 mt-2">Chargement des données...</p>
        </div>

        <div *ngIf="!loadingRows" class="card">
            <div class="table-container">
                <div class="overflow-x-auto">
                    <table class="table" *ngIf="rows.length > 0">
                        <thead>
                            <tr>
                                <th *ngFor="let column of columns.slice(0, maxVisibleColumns)" [title]="column">
                                  {{ column.length > 15 ? column.substring(0, 15) + '...' : column }}
                                </th>
                                <th *ngIf="columns.length > maxVisibleColumns" class="text-center">
                                  +{{ columns.length - maxVisibleColumns }}
                                </th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr *ngFor="let row of rows">
                                <td *ngFor="let column of columns.slice(0, maxVisibleColumns)" [title]="row.data[column]">
                                  {{ truncateText(row.data[column], 20) }}
                                </td>
                                <td *ngIf="columns.length > maxVisibleColumns" class="text-center">
                                  <button (click)="showEditModal(row)" class="btn btn-outline btn-sm">Voir</button>
                                </td>
                                <td>
                                    <div class="btn-group">
                                        <button (click)="showEditModal(row)" class="btn btn-primary btn-sm" title="Modifier">
                                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                                          </svg>
                                        </button>
                                        <button (click)="deleteRow(row.id)" class="btn btn-danger btn-sm" title="Supprimer">
                                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                                                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                                          </svg>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div *ngIf="rows.length === 0" class="text-center py-8">
                    <div class="mb-4">
                      <svg class="w-12 h-12 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                      </svg>
                    </div>
                    <p class="text-gray-500">Aucune donnée trouvée pour les critères actuels.</p>
                </div>
            </div>
            <div *ngIf="totalPages > 1" class="pagination mt-4">
                <button class="pagination-btn" [disabled]="currentPage === 0" (click)="goToPage(0)">Première</button>
                <button class="pagination-btn" [disabled]="currentPage === 0" (click)="goToPage(currentPage - 1)">Préc.</button>
                <span class="px-3 py-2 text-gray-700">Page {{ currentPage + 1 }} / {{ totalPages }}</span>
                <button class="pagination-btn" [disabled]="currentPage >= totalPages - 1" (click)="goToPage(currentPage + 1)">Suiv.</button>
                <button class="pagination-btn" [disabled]="currentPage >= totalPages - 1" (click)="goToPage(totalPages - 1)">Dernière</button>
            </div>
        </div>
    </div>

    <div *ngIf="activeView === 'history'" class="slide-up" style="animation-delay: 0.2s;">
        <div class="card">
            <h3 class="card-title mb-4">Dernières modifications pour la feuille "{{ selectedSheet?.sheetName }}"</h3>
            <div *ngIf="loadingHistory" class="text-center py-8">
                <div class="loading-spinner"></div>
            </div>
            <div *ngIf="!loadingHistory && sheetHistory.length === 0" class="text-center py-8">
                <div class="mb-4">
                  <svg class="w-12 h-12 mx-auto text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
                <p class="text-gray-500">Aucun historique de modification pour cette feuille.</p>
            </div>
            <div *ngIf="!loadingHistory && sheetHistory.length > 0" class="space-y-4">
                <div *ngFor="let item of sheetHistory" class="history-item"
                     [class.create]="item.operationType === 'CREATE'"
                     [class.update]="item.operationType === 'UPDATE'"
                     [class.delete]="item.operationType === 'DELETE'">
                    <div class="flex items-center justify-between">
                        <span class="badge"
                            [class.badge-success]="item.operationType === 'CREATE'"
                            [class.badge-info]="item.operationType === 'UPDATE'"
                            [class.badge-error]="item.operationType === 'DELETE'">
                            {{ getOperationLabel(item.operationType) }}
                        </span>
                        <span class="text-xs text-gray-500">{{ formatDate(item.timestamp) }}</span>
                    </div>
                    <p class="text-sm text-gray-700 mt-1">Ligne ID: <strong>{{ item.rowEntityId }}</strong></p>
                    <div *ngIf="item.operationType === 'UPDATE'">
                        <details class="text-xs mt-1 cursor-pointer">
                            <summary class="hover:text-gray-800">Voir les détails</summary>
                            <div class="grid grid-cols-2 gap-2 mt-2">
                                <div>
                                    <p class="font-semibold">Avant :</p>
                                    <pre class="bg-gray-100 p-2 rounded text-xs">{{ formatJson(item.oldData) }}</pre>
                                </div>
                                <div>
                                    <p class="font-semibold">Après :</p>
                                    <pre class="bg-red-50 p-2 rounded text-xs">{{ formatJson(item.newData) }}</pre>
                                </div>
                            </div>
                        </details>
                    </div>
                </div>
                <div *ngIf="historyTotalPages > 1" class="pagination">
                    <button class="pagination-btn" [disabled]="historyCurrentPage === 0" (click)="goToHistoryPage(historyCurrentPage - 1)">Précédente</button>
                    <span class="px-3 py-2 text-gray-700">Page {{ historyCurrentPage + 1 }} / {{ historyTotalPages }}</span>
                    <button class="pagination-btn" [disabled]="historyCurrentPage >= historyTotalPages - 1" (click)="goToHistoryPage(historyCurrentPage + 1)">Suivante</button>
                </div>
            </div>
        </div>
    </div>
</div>

<div *ngIf="loading" class="text-center py-16">
    <div class="loading-spinner" style="width: 3rem; height: 3rem;"></div>
    <p class="text-gray-600 mt-4 text-lg">Chargement du fichier et de ses feuilles...</p>
</div>

<app-row-edit-modal *ngIf="isShowingEditModal" [row]="selectedRow" [columns]="columns" [isEditMode]="isEditMode"
    (closeModal)="closeEditModal()" (saveRow)="saveRow($event)">
</app-row-edit-modal>

<app-graph-modal *ngIf="isShowingGraphModal && selectedSheet" 
    [sheetId]="selectedSheet.id" 
    [columns]="columns" 
    [sampleRows]="rows"
    (closeModal)="isShowingGraphModal = false">
</app-graph-modal>
`,
  styles: [`
.space-x-8 > :not([hidden]) ~ :not([hidden]) {
  margin-left: 2rem;
}

.table-container {
  max-height: 65vh;
  overflow-y: auto;
}

.w-4 { width: 1rem; }
.h-4 { height: 1rem; }
.w-12 { width: 3rem; }
.h-12 { height: 3rem; }
.mx-auto { margin-left: auto; margin-right: auto; }
.overflow-x-auto { overflow-x: auto; }
.flex-wrap { flex-wrap: wrap; }
.space-y-4 > * + * { margin-top: 1rem; }
.grid { display: grid; }
.grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.gap-2 { gap: 0.5rem; }
.mt-2 { margin-top: 0.5rem; }
.mt-4 { margin-top: 1rem; }
.bg-red-50 { background-color: #fef2f2; }
.text-xs { font-size: 0.75rem; }
`]
})
export class FileProcessingComponent implements OnInit {
  fileId!: number;
  file: FileEntity | null = null;
  
  sheets: SheetEntity[] = [];
  selectedSheet: SheetEntity | null = null;

  rows: RowEntity[] = [];
  columns: string[] = [];
  
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 50;
  pageSizes: number[] = [25, 50, 100, 200];
  
  searchKeyword = '';
  sortField = '';
  sortDirection = 'asc';
  
  loading = true;
  loadingRows = false;
  
  isShowingEditModal = false;
  isShowingGraphModal = false;
  isShowingHistoryModal = false;
  
  selectedRow: RowEntity | null = null;
  isEditMode = false;
  rowHistory: ModificationHistory[] = [];
  loadingHistory = false;
  
  activeView: 'data' | 'history' = 'data';
  sheetHistory: ModificationHistory[] = [];
  historyCurrentPage = 0;
  historyTotalPages = 0;
  historyPageSize = 15;
  maxVisibleColumns = 6;

  private searchTimeout: any;
  Math = Math;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fileService: FileService,
    private rowService: RowService
  ) {}

  ngOnInit() {
    this.fileId = Number(this.route.snapshot.params['id']);
    this.loadFileAndSheets();
  }

  loadFileAndSheets() {
    this.loading = true;
    this.fileService.getFile(this.fileId).subscribe({
      next: (file: FileEntity) => {
        this.file = file;
        this.fileService.getSheets(this.fileId).subscribe({
          next: (sheets: SheetEntity[]) => {
            this.sheets = sheets;
            if (this.sheets.length > 0) {
              this.selectSheet(this.sheets[0]);
            } else {
              this.loading = false;
            }
          },
          error: (error: any) => {
            console.error('Erreur chargement feuilles:', error);
            this.loading = false;
          }
        });
      },
      error: (error: any) => {
        console.error('Erreur chargement fichier:', error);
        this.loading = false;
        this.router.navigate(['/']);
      }
    });
  }

  selectSheet(sheet: SheetEntity) {
    if (this.selectedSheet?.id === sheet.id) return;
    this.activeView = 'data';
    this.selectedSheet = sheet;
    this.columns = sheet.headersJson ? JSON.parse(sheet.headersJson) : [];
    this.currentPage = 0;
    this.sortField = '';
    this.sortDirection = 'asc';
    this.searchKeyword = '';
    this.loadRowsForSelectedSheet();
  }

  loadRowsForSelectedSheet() {
    if (!this.selectedSheet) return;

    this.loadingRows = true;
    
    let sort: string | undefined;
    if (this.sortField) {
      sort = `data.${this.sortField},${this.sortDirection}`;
    }

    this.rowService.getRowsForSheet(
      this.selectedSheet.id,
      this.currentPage,
      this.pageSize,
      this.searchKeyword || undefined,
      sort
    ).subscribe({
      next: (response: PageResponse<RowEntity>) => {
        this.rows = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.loadingRows = false;
        if(this.loading) this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur chargement lignes:', error);
        this.loadingRows = false;
        this.loading = false;
      }
    });
  }
  
  onSearchInput(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
        this.currentPage = 0;
        this.loadRowsForSelectedSheet();
    }, 500);
  }

  onSortOrPageSizeChange() {
    this.currentPage = 0;
    this.loadRowsForSelectedSheet();
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadRowsForSelectedSheet();
    }
  }

  selectHistoryView() {
    this.activeView = 'history';
    this.historyCurrentPage = 0;
    this.loadHistoryForSelectedSheet();
  }

  loadHistoryForSelectedSheet() {
    if (!this.selectedSheet) return;
    this.loadingHistory = true;
    this.rowService.getHistoryForSheet(this.selectedSheet.id, this.historyCurrentPage, this.historyPageSize)
      .subscribe({
        next: (response) => {
          this.sheetHistory = response.content;
          this.historyTotalPages = response.totalPages;
          this.loadingHistory = false;
        },
        error: (err: any) => {
          console.error("Erreur lors du chargement de l'historique de la feuille", err);
          this.loadingHistory = false;
        }
      });
  }

  goToHistoryPage(page: number) {
    if (page >= 0 && page < this.historyTotalPages) {
      this.historyCurrentPage = page;
      this.loadHistoryForSelectedSheet();
    }
  }

  saveRow(row: Partial<RowEntity>) {
    if (!this.selectedSheet) return;

    const action = this.isEditMode && row.id
      ? this.rowService.updateRow(row.id, row)
      : this.rowService.createRow(this.selectedSheet.id, row);

    action.subscribe({
      next: () => {
        this.closeEditModal();
        this.loadRowsForSelectedSheet();
      },
      error: (error: any) => console.error('Erreur lors de la sauvegarde:', error)
    });
  }

  deleteRow(rowId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette ligne ?')) {
      this.rowService.deleteRow(rowId).subscribe({
        next: () => {
          this.loadRowsForSelectedSheet();
        },
        error: (error: any) => console.error('Erreur lors de la suppression:', error)
      });
    }
  }

  downloadCurrentSheet() {
    if (!this.selectedSheet || !this.file) return;
    this.fileService.downloadSheet(this.selectedSheet.id, this.selectedSheet.sheetName, this.searchKeyword)
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `${this.file?.fileName} - ${this.selectedSheet?.sheetName}.xlsx`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        },
        error: (error: any) => {
          console.error('Erreur lors du téléchargement:', error);
        }
      });
  }

  goBack() { this.router.navigate(['/']); }
  formatDate(dateString: string): string { return new Date(dateString).toLocaleString('fr-FR'); }
  showAddModal() { this.selectedRow = null; this.isEditMode = false; this.isShowingEditModal = true; }
  showEditModal(row: RowEntity) { this.selectedRow = row; this.isEditMode = true; this.isShowingEditModal = true; }
  closeEditModal() { this.isShowingEditModal = false; this.selectedRow = null; }
  truncateText(text: any, maxLength: number): string {
    if (typeof text !== 'string') {
      text = text !== undefined && text !== null ? String(text) : '';
    }
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }
  getOperationLabel(operation: string): string {
    switch (operation) {
      case 'CREATE': return 'Création';
      case 'UPDATE': return 'Modification';
      case 'DELETE': return 'Suppression';
      default: return operation;
    }
  }
  formatJson(jsonString: string | null): string {
    if (!jsonString) return '(vide)';
    try {
      return JSON.stringify(JSON.parse(jsonString), null, 2);
    } catch {
      return jsonString;
    }
  }
}