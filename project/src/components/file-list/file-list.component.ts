import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FileService } from '../../services/file.service';
import { FileEntity, PageResponse } from '../../models/file.model';

@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Fichiers Excel</h3>
      </div>

      <div *ngIf="loading" class="text-center py-8">
        <div class="loading-spinner"></div>
        <p class="text-gray-600 mt-2">Chargement des fichiers...</p>
      </div>

      <div *ngIf="!loading && files.length === 0" class="text-center py-8">
        <p class="text-gray-500">Aucun fichier trouvé. Commencez par télécharger un fichier Excel.</p>
      </div>

      <div *ngIf="!loading && files.length > 0" class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>Nom du fichier</th>
              <th>Date de téléchargement</th>
              <th>Nombre de lignes</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let file of files" class="hover:bg-gray-50">
              <td class="font-medium">{{ file.fileName }}</td>
              <td>{{ formatDate(file.uploadTimestamp) }}</td>
              <td>
                <span class="badge badge-info">{{ file.totalRows }} lignes</span>
              </td>
              <td>
                <div class="flex gap-2">
                  <button (click)="viewFile(file.id)" class="btn btn-primary btn-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                  </button>
                  <button (click)="downloadFile(file.fileName)" class="btn btn-success btn-sm">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    
                  </button>
                  <button (click)="deleteFile(file.id)" class="btn btn-danger btn-sm">
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
  `]
})
export class FileListComponent implements OnInit {
  files: FileEntity[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 0;
  pageSize = 10;

  constructor(
    private fileService: FileService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadFiles();
  }

  loadFiles() {
    this.loading = true;
    this.fileService.getFiles(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponse<FileEntity>) => {
        this.files = response.content;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fichiers:', error);
        this.loading = false;
      }
    });
  }

  viewFile(fileId: number) {
    this.router.navigate(['/file', fileId]);
  }

  downloadFile(fileName: string) {
    this.fileService.downloadFile(fileName).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
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

  deleteFile(fileId: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce fichier ? Cette action est irréversible.')) {
      this.fileService.deleteFile(fileId).subscribe({
        next: () => {
          this.loadFiles();
        },
        error: (error) => {
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