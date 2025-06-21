import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExcelService } from '../../services/excel.service';
import { RowEntity, PageResponse, ModificationHistory } from '../../models/row-entity.model';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="data-table-container card fade-in">
      <!-- Header with search and actions -->
      <div class="table-header">
        <div class="header-left">
          <h2>📋 Data Management</h2>
          <div class="data-stats" *ngIf="pageData">
            <span class="badge badge-success">{{ pageData.totalElements }} Total Records</span>
            <span class="badge" [class.badge-warning]="pageData.totalPages > 1">
              Page {{ pageData.pageable.pageNumber + 1 }} of {{ pageData.totalPages }}
            </span>
          </div>
        </div>
        
        <div class="header-actions">
          <button class="btn btn-success btn-sm" (click)="downloadData()" [disabled]="isLoading">
            <span>📥</span> Export Excel
          </button>
          <button (click)="openAllHistoryModal()" class="button-secondary" [disabled]="isLoadingAllHistory">
            <i class="fas fa-history"></i>
            <span *ngIf="!isLoadingAllHistory">Historique Global</span>
            <span *ngIf="isLoadingAllHistory">Chargement...</span>
          </button>
          <button class="btn btn-danger btn-sm" (click)="showResetConfirm = true" [disabled]="isLoading">
            <span>🗑️</span> Reset All
          </button>
        </div>

      </div>

      <!-- Search and filters -->
      <div class="search-section">
        <div class="search-row">
          <div class="search-field">
            <input 
              type="text" 
              class="input" 
              placeholder="🔍 Search by filename..."
              [(ngModel)]="searchFileName"
              (input)="onSearchChange()">
          </div>
          <div class="search-field">
            <input 
              type="text" 
              class="input" 
              placeholder="🔍 Search in data..."
              [(ngModel)]="searchKeyword"
              (input)="onSearchChange()">
          </div>
          <button class="btn btn-secondary" (click)="clearSearch()" *ngIf="hasActiveSearch()">
            Clear
          </button>
        </div>
      </div>

      <!-- Loading state -->
      <div class="loading-container" *ngIf="isLoading">
        <div class="loading-spinner">
          <div class="loading"></div>
          <p>Loading data...</p>
        </div>
      </div>

      <!-- Data table -->
      <div class="table-container" *ngIf="!isLoading && pageData && pageData.content.length > 0">
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Sheet</th>
                <th *ngFor="let column of getColumns()">{{ column }}</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of pageData.content; trackBy: trackByRowId" 
                  [class.editing]="editingRowId === row.id">
                <td>
                  <span class="row-id">#{{ row.id }}</span>
                </td>
                <td>
                  <span class="badge">Sheet {{ row.sheetIndex + 1 }}</span>
                </td>
                <td *ngFor="let column of getColumns()">
                  <div *ngIf="editingRowId !== row.id" class="cell-content">
                    {{ getCellValue(row, column) }}
                  </div>
                  <input 
                    *ngIf="editingRowId === row.id"
                    type="text" 
                    class="input cell-input"
                    [value]="getCellValue(row, column)"
                    (input)="updateEditingData(column, $event)">
                </td>
                <td>
                  <div class="action-buttons">
                    <button 
                      *ngIf="editingRowId !== row.id"
                      class="btn btn-sm btn-secondary" 
                      (click)="startEdit(row)"
                      title="Edit">
                      ✏️
                    </button>
                    <button 
                      *ngIf="editingRowId === row.id"
                      class="btn btn-sm btn-success" 
                      (click)="saveEdit()"
                      [disabled]="isSaving"
                      title="Save">
                      💾
                    </button>
                    <button 
                      *ngIf="editingRowId === row.id"
                      class="btn btn-sm btn-secondary" 
                      (click)="cancelEdit()"
                      title="Cancel">
                      ❌
                    </button>
                    <button 
                      *ngIf="editingRowId !== row.id"
                      class="btn btn-sm btn-warning" 
                      (click)="showHistory(row.id!)"
                      title="History">
                      📜
                    </button>
                    <button 
                      *ngIf="editingRowId !== row.id"
                      class="btn btn-sm btn-danger" 
                      (click)="deleteRow(row.id!)"
                      [disabled]="isDeleting === row.id"
                      title="Delete">
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Empty state -->
      <div class="empty-state" *ngIf="!isLoading && (!pageData || pageData.content.length === 0)">
        <div class="empty-content">
          <div class="empty-icon">📊</div>
          <h3>No Data Found</h3>
          <p *ngIf="hasActiveSearch()">Try adjusting your search criteria</p>
          <p *ngIf="!hasActiveSearch()">Upload an Excel file to get started</p>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-container" *ngIf="pageData && pageData.totalPages > 1">
        <div class="pagination">
          <button 
            class="btn btn-sm btn-secondary" 
            (click)="goToPage(0)"
            [disabled]="pageData.first">
            ⏮️
          </button>
          <button 
            class="btn btn-sm btn-secondary" 
            (click)="goToPage(currentPage - 1)"
            [disabled]="pageData.first">
            ⏪
          </button>
          
          <span class="page-info">
            Page {{ pageData.pageable.pageNumber + 1 }} of {{ pageData.totalPages }}
          </span>
          
          <button 
            class="btn btn-sm btn-secondary" 
            (click)="goToPage(currentPage + 1)"
            [disabled]="pageData.last">
            ⏩
          </button>
          <button 
            class="btn btn-sm btn-secondary" 
            (click)="goToPage(pageData.totalPages - 1)"
            [disabled]="pageData.last">
            ⏭️
          </button>
        </div>
        
        <div class="page-size-selector">
          <label>Rows per page:</label>
          <select class="select" [(ngModel)]="pageSize" (change)="onPageSizeChange()">
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
            <option value="100">100</option>
          </select>
        </div>
      </div>
    </div>

    <!-- History Modal -->
    <div class="modal-overlay" *ngIf="showHistoryModal" (click)="closeHistoryModal()">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>📜 Modification History</h3>
          <button class="btn btn-sm btn-secondary" (click)="closeHistoryModal()">✕</button>
        </div>
        <div class="modal-body">
          <div *ngIf="historyData && historyData.length > 0" class="history-list">
            <div *ngFor="let history of historyData" class="history-item">
              <div class="history-header">
                <span class="badge" [ngClass]="getHistoryBadgeClass(history.operationType)">
                  {{ history.operationType }}
                </span>
                <span class="history-date">{{ formatDate(history.timestamp) }}</span>
              </div>
              <div class="history-content">
            <div *ngIf="history.operationType === 'UPDATE'">
                <div class="history-comparison">
                    <div>
                        <h5>Avant</h5>
                        <pre>{{ formatHistoryData(history.oldData) }}</pre>
                    </div>
                    <div>
                        <h5>Après</h5>
                        <pre>{{ formatHistoryData(history.newData) }}</pre>
                    </div>
                </div>
            </div>

            <div *ngIf="history.operationType === 'CREATE'">
                <h5>Données Créées</h5>
                <pre>{{ formatHistoryData(history.newData) }}</pre>
            </div>

            <div *ngIf="history.operationType === 'DELETE'">
                <p>Cette ligne a été supprimée.</p>
                <h5 *ngIf="history.oldData">Données supprimées</h5>
                <pre *ngIf="history.oldData">{{ formatHistoryData(history.oldData) }}</pre>
            </div>
            </div>
            </div>
          </div>
          <div *ngIf="!historyData || historyData.length === 0" class="empty-history">
            <p>No modification history found</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Reset Confirmation Modal -->
    <div class="modal-overlay" *ngIf="showResetConfirm" (click)="showResetConfirm = false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>⚠️ Confirm Reset</h3>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to delete ALL data and history?</p>
          <p><strong>This action cannot be undone!</strong></p>
          <div class="modal-actions">
            <button class="btn btn-secondary" (click)="showResetConfirm = false">Cancel</button>
            <button class="btn btn-danger" (click)="confirmReset()" [disabled]="isResetting">
              <span *ngIf="!isResetting">🗑️ Delete All</span>
              <span *ngIf="isResetting">Deleting...</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showAllHistoryModal">
  <div class="modal-container large">
    <div class="modal-header">
      <h3>Historique Global des Modifications</h3>
      <button class="close-button" (click)="closeAllHistoryModal()">&times;</button>
    </div>
    <div class="modal-body">
      <div *ngIf="isLoadingAllHistory" class="loading-indicator">
        <p>Chargement de l'historique...</p>
      </div>

      <div *ngIf="!isLoadingAllHistory && allHistoryData.length === 0" class="empty-state">
        <p>Aucune modification n'a encore été enregistrée.</p>
      </div>

      <ul *ngIf="!isLoadingAllHistory && allHistoryData.length > 0" class="history-list">
        <li *ngFor="let history of allHistoryData" class="history-list-item">
          
          <div class="history-item-header">
            <span class="history-op-badge" [ngClass]="history.operationType">{{ history.operationType }}</span>
            <span class="history-entity">Ligne ID: <strong>{{ history.id }}</strong></span>
            <span class="history-timestamp">{{ history.timestamp | date:'dd/MM/yyyy à HH:mm:ss' }}</span>
          </div>

          <div class="history-content">
              <div *ngIf="history.operationType === 'UPDATE'">
                  <div class="history-comparison">
                      <div>
                          <h5>Avant</h5>
                          <pre>{{ formatHistoryData(history.oldData) }}</pre>
                      </div>
                      <div>
                          <h5>Après</h5>
                          <pre>{{ formatHistoryData(history.newData) }}</pre>
                      </div>
                  </div>
              </div>
              <div *ngIf="history.operationType === 'CREATE'">
                  <h5>Données Créées</h5>
                  <pre>{{ formatHistoryData(history.newData) }}</pre>
              </div>
              <div *ngIf="history.operationType === 'DELETE'">
                  <h5>Données Supprimées</h5>
                  <pre>{{ formatHistoryData(history.oldData) }}</pre>
              </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
</div>
  `,
  styles: [`
  .history-list {
  list-style-type: none;
  padding: 0;
  margin: 0;
  max-height: 60vh;
  overflow-y: auto;
}

.history-list-item {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.history-item-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.history-op-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  color: white;
}
.history-op-badge.CREATE { background-color: #28a745; }
.history-op-badge.UPDATE { background-color: #007bff; }
.history-op-badge.DELETE { background-color: #dc3545; }

.history-timestamp {
  font-size: 12px;
  color: #6c757d;
  margin-left: auto;
}

.modal-container.large {
  max-width: 900px;
}

    .data-table-container {
      padding: 32px;
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 32px;
      gap: 24px;
    }

    .header-left h2 {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 12px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-purple));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .data-stats {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .header-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .search-section {
      margin-bottom: 32px;
      padding: 24px;
      background: var(--bg-secondary);
      border-radius: 12px;
      border: 1px solid var(--border-color);
    }

    .search-row {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
    }

    .search-field {
      flex: 1;
      min-width: 200px;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 80px 0;
    }

    .loading-spinner {
      text-align: center;
    }

    .loading-spinner p {
      margin-top: 16px;
      color: var(--text-secondary);
    }

    .table-container {
      margin-bottom: 32px;
    }

    .table-wrapper {
      overflow-x: auto;
      border-radius: 12px;
      border: 1px solid var(--border-color);
    }

    .table {
      margin: 0;
    }

    .table th {
      position: sticky;
      top: 0;
      z-index: 10;
      white-space: nowrap;
    }

    .table td {
      vertical-align: middle;
    }

    .row-id {
      font-family: 'Courier New', monospace;
      font-weight: 600;
      color: var(--accent-primary);
    }

    .cell-content {
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .cell-input {
      width: 100%;
      min-width: 120px;
    }

    .action-buttons {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .action-buttons .btn {
      min-width: 36px;
      padding: 6px 8px;
    }

    .editing {
      background: rgba(59, 130, 246, 0.1) !important;
      border: 1px solid var(--accent-primary);
    }

    .empty-state {
      text-align: center;
      padding: 80px 32px;
    }

    .empty-content {
      max-width: 400px;
      margin: 0 auto;
    }

    .empty-icon {
      font-size: 64px;
      margin-bottom: 24px;
      opacity: 0.5;
    }

    .empty-content h3 {
      font-size: 24px;
      margin-bottom: 12px;
      color: var(--text-primary);
    }

    .empty-content p {
      color: var(--text-secondary);
      font-size: 16px;
    }

    .pagination-container {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px 0;
      border-top: 1px solid var(--border-color);
      flex-wrap: wrap;
      gap: 16px;
    }

    .pagination {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .page-info {
      margin: 0 16px;
      font-weight: 500;
      color: var(--text-primary);
    }

    .page-size-selector {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .page-size-selector label {
      font-size: 14px;
      color: var(--text-secondary);
    }

    .page-size-selector .select {
      width: auto;
      min-width: 80px;
    }

    .modal {
      max-width: 800px;
      width: 90vw;
      max-height: 80vh;
      display: flex;
      flex-direction: column;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      border-bottom: 1px solid var(--border-color);
    }

    .modal-header h3 {
      margin: 0;
      font-size: 20px;
    }

    .modal-body {
      padding: 24px;
      overflow-y: auto;
      flex: 1;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
    }

    .history-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .history-item {
      padding: 16px;
      background: var(--bg-secondary);
      border-radius: 8px;
      border: 1px solid var(--border-color);
    }

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .history-date {
      font-size: 12px;
      color: var(--text-muted);
    }

    .history-content pre {
      background: var(--bg-primary);
      padding: 12px;
      border-radius: 6px;
      font-size: 12px;
      overflow-x: auto;
      margin: 0;
      border: 1px solid var(--border-color);
    }

    .history-comparison {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    }
    .history-comparison h5 {
        font-size: 14px;
        margin-bottom: 8px;
        color: var(--text-secondary);
    }

    .empty-history {
      text-align: center;
      padding: 40px;
      color: var(--text-secondary);
    }

    @media (max-width: 768px) {
      .data-table-container {
        padding: 16px;
      }

      .table-header {
        flex-direction: column;
        align-items: stretch;
      }

      .header-actions {
        justify-content: flex-start;
      }

      .search-row {
        flex-direction: column;
      }

      .search-field {
        min-width: unset;
      }

      .pagination-container {
        flex-direction: column;
        text-align: center;
      }

      .action-buttons {
        justify-content: center;
      }

      .modal {
        width: 95vw;
        max-height: 95vh;
      }
    }
  `]
})
export class DataTableComponent implements OnInit {
  @Input() refreshTrigger: any;

  pageData: PageResponse<RowEntity> | null = null;
  isLoading = false;
  currentPage = 0;
  pageSize = 25;
  searchFileName = '';
  searchKeyword = '';
  searchTimeout: any;

  // Editing state
  editingRowId: number | null = null;
  editingData: any = {};
  isSaving = false;
  isDeleting: number | null = null;

  // History modal
  showHistoryModal = false;
  historyData: ModificationHistory[] = [];

  // Reset confirmation
  showResetConfirm = false;
  isResetting = false;

  //allhistory
  showAllHistoryModal = false;
  allHistoryData: ModificationHistory[] = [];
  isLoadingAllHistory = false;

  constructor(private excelService: ExcelService) {}

  ngOnInit() {
    this.loadData();
  }

  ngOnChanges() {
    if (this.refreshTrigger) {
      this.loadData();
    }
  }

  loadData() {
    this.isLoading = true;
    
    const fileName = this.searchFileName.trim() || undefined;
    const keyword = this.searchKeyword.trim() || undefined;

    this.excelService.getRows(this.currentPage, this.pageSize, fileName, keyword)
      .subscribe({
        next: (data) => {
          this.pageData = data;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading data:', error);
          this.isLoading = false;
        }
      });
  }

  onSearchChange() {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.loadData();
    }, 500);
  }

  clearSearch() {
    this.searchFileName = '';
    this.searchKeyword = '';
    this.currentPage = 0;
    this.loadData();
  }

  hasActiveSearch(): boolean {
    return this.searchFileName.trim() !== '' || this.searchKeyword.trim() !== '';
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadData();
  }

  onPageSizeChange() {
    this.currentPage = 0;
    this.loadData();
  }

  getColumns(): string[] {
    if (!this.pageData || this.pageData.content.length === 0) return [];
    
    const firstRow = this.pageData.content[0];
    return Object.keys(firstRow.data);
  }

  getCellValue(row: RowEntity, column: string): string {
    const value = row.data[column];
    return value !== null && value !== undefined ? String(value) : '';
  }

  trackByRowId(index: number, row: RowEntity): number {
    return row.id || index;
  }

  startEdit(row: RowEntity) {
    this.editingRowId = row.id!;
    this.editingData = { ...row.data };
  }

  updateEditingData(column: string, event: any) {
    this.editingData[column] = event.target.value;
  }

  saveEdit() {
    if (!this.editingRowId) return;

    this.isSaving = true;
    const updatedRow: RowEntity = {
      id: this.editingRowId,
      sheetIndex: this.pageData!.content.find(r => r.id === this.editingRowId)!.sheetIndex,
      data: this.editingData
    };

    this.excelService.updateRow(this.editingRowId, updatedRow).subscribe({
      next: () => {
        this.isSaving = false;
        this.editingRowId = null;
        this.editingData = {};
        this.loadData();
      },
      error: (error) => {
        console.error('Error updating row:', error);
        this.isSaving = false;
      }
    });
  }

  cancelEdit() {
    this.editingRowId = null;
    this.editingData = {};
  }

  openAllHistoryModal() {
    this.isLoadingAllHistory = true;
    this.excelService.getAllHistory().subscribe({
      next: (data) => {
        this.allHistoryData = data;
        this.isLoadingAllHistory = false;
        this.showAllHistoryModal = true;
      },
      error: (err) => {
        console.error('Erreur lors du chargement de l\'historique global', err);
        alert('Impossible de charger l\'historique.');
        this.isLoadingAllHistory = false;
      }
    });
  }

  // NOUVELLE MÉTHODE : pour fermer la modale
  closeAllHistoryModal() {
    this.showAllHistoryModal = false;
    this.allHistoryData = []; // Libérer la mémoire
  }

  deleteRow(id: number) {
    if (confirm('Are you sure you want to delete this row?')) {
      this.isDeleting = id;
      this.excelService.deleteRow(id).subscribe({
        next: () => {
          this.isDeleting = null;
          this.loadData();
        },
        error: (error) => {
          console.error('Error deleting row:', error);
          this.isDeleting = null;
        }
      });
    }
  }

  showHistory(rowId: number) {
    this.excelService.getHistory(rowId).subscribe({
      next: (history) => {
        this.historyData = history;
        this.showHistoryModal = true;
      },
      error: (error) => {
        console.error('Error loading history:', error);
      }
    });
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
    this.historyData = [];
  }

  getHistoryBadgeClass(operationType: string): string {
    switch (operationType) {
      case 'CREATE': return 'badge-success';
      case 'UPDATE': return 'badge-warning';
      case 'DELETE': return 'badge-error';
      default: return '';
    }
  }

  formatDate(timestamp: string): string {
    return new Date(timestamp).toLocaleString();
  }

  formatHistoryData(data: string | undefined): string {
    if (!data) return 'No data';
    try {
      return JSON.stringify(JSON.parse(data), null, 2);
    } catch {
      return data;
    }
  }

  downloadData() {
    const fileName = this.searchFileName.trim() || undefined;
    const keyword = this.searchKeyword.trim() || undefined;

    this.excelService.downloadExcel(fileName, keyword).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'export.xlsx';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading file:', error);
      }
    });
  }

  // Dans DataTableComponent

confirmReset() {
  this.isResetting = true;
  this.excelService.resetAll().subscribe({
    next: (responseMessage) => {
      this.isResetting = false;
      this.showResetConfirm = false;
      
      // 1. Signaler que la suppression est terminée
      alert(responseMessage); // Simple alerte, ou utilisez un service de notification (toastr, snackbar)

      // 2. Actualiser la page pour un état neutre
      window.location.reload(); 
    },
    error: (error) => {
      console.error('Error resetting data:', error);
      this.isResetting = false;
      alert('Une erreur est survenue lors de la réinitialisation.'); // Notifier l'utilisateur de l'erreur
    }
  });
}
}