import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService } from '../../services/file.service';
import { UploadResponse } from '../../models/file.model';

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
      <div class="loading-spinner" style="width: 2rem; height: 2rem;"></div>
      <p class="text-gray-600 mt-2">Téléchargement en cours...</p>
    </div>

    <div *ngIf="uploadMessage" class="notification"
         [class.notification-success]="uploadSuccessful"
         [class.notification-error]="!uploadSuccessful"
         [class.show]="showNotification">
      <div class="flex items-center">
        <svg *ngIf="uploadSuccessful" class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
        </svg>
        <svg *ngIf="!uploadSuccessful" class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
        {{ uploadMessage }}
      </div>
    </div>
  `,
  styles: [`
    .w-12 { width: 3rem; }
    .h-12 { height: 3rem; }
    .w-5 { width: 1.25rem; }
    .h-5 { height: 1.25rem; }
    .mx-auto { margin-left: auto; margin-right: auto; }
    .mr-2 { margin-right: 0.5rem; }
    .mt-4 { margin-top: 1rem; }
    .mt-2 { margin-top: 0.5rem; }
    .mb-2 { margin-bottom: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .text-gray-400 { color: var(--gray-400); }
    .text-gray-500 { color: var(--gray-500); }
    .text-gray-600 { color: var(--gray-600); }
    .text-gray-700 { color: var(--gray-700); }
    .text-center { text-align: center; }
    .text-lg { font-size: 1.125rem; }
    .text-sm { font-size: 0.875rem; }
    .font-medium { font-weight: 500; }
  `]
})
export class FileUploadComponent {
  @Output() uploadSuccess = new EventEmitter<void>();
  
  isDragOver = false;
  isUploading = false;
  uploadMessage = '';
  uploadSuccessful = false;
  showNotification = false;

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
      this.showNotificationMessage('Format de fichier non supporté. Veuillez sélectionner un fichier Excel (.xlsx ou .xls).', false);
      return;
    }

    this.isUploading = true;
    this.hideNotification();

    this.fileService.uploadFile(file).subscribe({
      next: (response: UploadResponse) => {
        this.isUploading = false;
        this.showNotificationMessage(response.message, response.success);
        
        if (response.success) {
          this.uploadSuccess.emit();
        }
      },
      error: (error: any) => {
        this.isUploading = false;
        this.showNotificationMessage(
          'Erreur lors du téléchargement: ' + (error.error?.message || 'Erreur inconnue'), 
          false
        );
      }
    });
  }

  private showNotificationMessage(message: string, success: boolean) {
    this.uploadMessage = message;
    this.uploadSuccessful = success;
    this.showNotification = true;
    
    setTimeout(() => {
      this.hideNotification();
    }, 5000);
  }

  private hideNotification() {
    this.showNotification = false;
    setTimeout(() => {
      this.uploadMessage = '';
    }, 300);
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