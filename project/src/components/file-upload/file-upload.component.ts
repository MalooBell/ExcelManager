import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService } from '../../services/file.service';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="file-upload-area"
         [class.dragover]="isDragOver"
         (click)="fileInput.click()"
         (dragover)="onDragOver($event)"
         (dragleave)="onDragLeave($event)"
         (drop)="onDrop($event)">
      
      <input #fileInput type="file" accept=".xlsx,.xls" (change)="onFileSelected($event)" style="display: none;">
      
      <div class="text-center">
        <div class="mb-4">
          <svg class="w-12 h-12 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
          </svg>
        </div>
        <p class="text-lg font-medium text-gray-700 mb-2">
          {{ isDragOver ? 'Déposez le fichier ici' : 'Glissez-déposez votre fichier Excel ici' }}
        </p>
        <p class="text-gray-500 mb-4">ou cliquez pour sélectionner un fichier</p>
        <p class="text-sm text-gray-400">Formats supportés: .xlsx, .xls</p>
      </div>
    </div>

    <div *ngIf="isUploading" class="mt-4 text-center">
      <div class="loading-spinner"></div>
      <p class="text-gray-600 mt-2">Téléchargement en cours...</p>
    </div>

    <div *ngIf="uploadMessage" class="mt-4 p-4 rounded-lg" 
         [class.bg-green-50]="uploadSuccess" 
         [class.bg-red-50]="!uploadSuccess">
      <p [class.text-green-800]="uploadSuccess" [class.text-red-800]="!uploadSuccess">
        {{ uploadMessage }}
      </p>
    </div>
  `,
  styles: [`
    .file-upload-area {
      border: 2px dashed var(--gray-300);
      border-radius: var(--border-radius-lg);
      padding: var(--spacing-8);
      text-align: center;
      transition: all 0.2s ease;
      cursor: pointer;
    }

    .file-upload-area:hover {
      border-color: var(--primary-blue);
      background-color: var(--gray-50);
    }

    .file-upload-area.dragover {
      border-color: var(--primary-blue);
      background-color: rgba(59, 130, 246, 0.1);
    }

    .w-12 { width: 3rem; }
    .h-12 { height: 3rem; }
    .mx-auto { margin-left: auto; margin-right: auto; }
    .text-gray-400 { color: var(--gray-400); }
    .text-gray-500 { color: var(--gray-500); }
    .text-gray-600 { color: var(--gray-600); }
    .text-gray-700 { color: var(--gray-700); }
    .text-green-800 { color: #166534; }
    .text-red-800 { color: #991b1b; }
    .bg-green-50 { background-color: #f0fdf4; }
    .bg-red-50 { background-color: #fef2f2; }
  `]
})
export class FileUploadComponent {
  @Output() uploadSuccess = new EventEmitter<void>();
  
  isDragOver = false;
  isUploading = false;
  uploadMessage = '';
  uploadSuccessful = false;

  constructor(private fileService: FileService) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.handleFile(file);
    }
  }

  private handleFile(file: File) {
    if (!this.isValidFile(file)) {
      this.uploadMessage = 'Format de fichier non supporté. Veuillez sélectionner un fichier Excel (.xlsx ou .xls).';
      this.uploadSuccessful = false;
      return;
    }

    this.isUploading = true;
    this.uploadMessage = '';

    this.fileService.uploadFile(file).subscribe({
      next: (response) => {
        this.isUploading = false;
        this.uploadMessage = response.message;
        this.uploadSuccessful = response.success;
        
        if (response.success) {
          this.uploadSuccess.emit();
        }
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadMessage = 'Erreur lors du téléchargement: ' + (error.error?.message || 'Erreur inconnue');
        this.uploadSuccessful = false;
      }
    });
  }

  private isValidFile(file: File): boolean {
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel',
      'application/octet-stream'
    ];
    return validTypes.includes(file.type) || file.name.endsWith('.xlsx') || file.name.endsWith('.xls');
  }
}