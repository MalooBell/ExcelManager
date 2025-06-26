import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FileService } from '../../services/file.service';
import { FileEntity, PageResponse } from '../../models/file.model';

@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Fichiers Excel</h3>
        <div class="w-full max-w-sm">
          <div class="flex items-center border border-gray-200 rounded-lg p-1">
            <input type="text" class="form-control" placeholder="Rechercher par nom de fichier..."
              [(ngModel)]="searchKeyword" (input)="onSearchInput()" />
            <button *ngIf="searchKeyword" (click)="clearSearch()" class="p-1 text-gray-400 hover:text-gray-600">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clip-rule="evenodd"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-8">
        <div class="loading-spinner"></div>
        <p class="text-gray-600 mt-2">Chargement des fichiers...</p>
      </div>

      <div *ngIf="!loading && files.length === 0" class="text-center py-8">
        <p class="text-gray-500">Aucun fichier trouvé. Commencez par télécharger un fichier Excel.</p>
      </div>

      <div class="card">
      <div *ngIf="!loading && files.length > 0" class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>Nom du fichier</th>
              <th>Date de téléchargement</th>
              <th>Nombre de feuilles</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let file of files" class="hover:bg-gray-50">
              <td class="font-medium">{{ file.fileName }}</td>
              <td>{{ formatDate(file.uploadTimestamp) }}</td>
              <td>
                <span class="badge badge-info">{{ file.sheetCount }} feuille(s)</span>
              </td>
<td>
                <div class="flex gap-2">
                  <button (click)="viewFile(file.id)" class="btn btn-primary btn-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z">
                      </path>
                    </svg>
                  </button>
                  <button (click)="deleteFile(file.id)" class="btn btn-danger btn-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16">
                      </path>
                    </svg>
                  </button>
                </div>
              </td>
              </tr>
          </tbody>
        </table>
      </div>
      
      </div>

      <div *ngIf="totalPages > 1" class="pagination">
        <button class="pagination-btn" [class.disabled]="currentPage === 0" (click)="goToPage(0)"
          [disabled]="currentPage === 0">
          Première
        </button>
        <button class="pagination-btn" [class.disabled]="currentPage === 0" (click)="goToPage(currentPage - 1)"
          [disabled]="currentPage === 0">
          Précédente
        </button>
        <span class="px-3 py-2 text-gray-700">
          Page {{ currentPage + 1 }} sur {{ totalPages }}
        </span>
        <button class="pagination-btn" [class.disabled]="currentPage === totalPages - 1"
          (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages - 1">
          Suivante
        </button>
        <button class="pagination-btn" [class.disabled]="currentPage === totalPages - 1"
          (click)="goToPage(totalPages - 1)" [disabled]="currentPage === totalPages - 1">
          Dernière
        </button>
      </div>
    </div>
  `,
  styles: [`
    .w-4 { width: 1rem; }
    .h-4 { height: 1rem; }
    .overflow-x-auto { overflow-x: auto; }
    .hover\\:bg-gray-50:hover { background-color: var(--gray-50); }
    .max-w-sm { max-width: 24rem; }
    .p-1 { padding: 0.25rem; }
    .hover\\:text-gray-600:hover { color: var(--gray-600); }
  `]
})
export class FileListComponent implements OnInit {
  files: FileEntity[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 0;
  pageSize = 10;
  
  searchKeyword: string = '';
  private searchTimeout: any;

  constructor(
    private fileService: FileService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFiles();
  }

  loadFiles() {
    this.loading = true;
    this.fileService.getFiles(this.currentPage, this.pageSize, this.searchKeyword).subscribe({
      next: (response: PageResponse<FileEntity>) => {
        this.files = response.content;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des fichiers:', error);
        this.loading = false;
      }
    });
  }
  
  onSearchInput(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
        this.currentPage = 0;
        this.loadFiles();
    }, 500);
  }

  clearSearch(): void {
    this.searchKeyword = '';
    this.currentPage = 0;
    this.loadFiles();
  }

  viewFile(fileId: number) {
    this.router.navigate(['/file', fileId]);
  }

  deleteFile(fileId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce fichier ? Cette action est irréversible.')) {
      this.fileService.deleteFile(fileId).subscribe({
        next: () => {
          this.loadFiles();
        },
        error: (error: any) => {
          console.error('Erreur lors de la suppression:', error);
        }
      });
    }
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadFiles();
    }
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
}