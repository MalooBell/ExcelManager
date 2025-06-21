import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExcelService } from '../../services/excel.service';
import { UploadResponse } from '../../models/row-entity.model';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upload-container card fade-in">
      <div class="upload-header">
        <h2>📊 Upload Excel File</h2>
        <p>Drag and drop your Excel file or click to browse</p>
      </div>
      
      <div 
        class="upload-zone"
        [class.dragover]="isDragOver"
        [class.uploading]="isUploading"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        (drop)="onDrop($event)"
        (click)="fileInput.click()">
        
        <div class="upload-content">
          <div class="upload-icon" [class.pulse]="isUploading">
            <span *ngIf="!isUploading">📁</span>
            <div *ngIf="isUploading" class="loading"></div>
          </div>
          
          <div class="upload-text">
            <span *ngIf="!isUploading && !selectedFile">
              Drop your Excel file here or <strong>click to browse</strong>
            </span>
            <span *ngIf="selectedFile && !isUploading">
              📄 {{ selectedFile.name }}
            </span>
            <span *ngIf="isUploading">
              Uploading... {{ uploadProgress }}%
            </span>
          </div>
          
          <div class="file-info" *ngIf="selectedFile && !isUploading">
            <small>{{ formatFileSize(selectedFile.size) }} • {{ getFileType(selectedFile.name) }}</small>
          </div>
        </div>
        
        <input 
          #fileInput
          type="file" 
          accept=".xlsx,.xls"
          (change)="onFileSelected($event)"
          style="display: none;">
      </div>
      
      <div class="upload-actions" *ngIf="selectedFile && !isUploading">
        <button class="btn btn-secondary btn-sm" (click)="clearFile()">
          Clear
        </button>
        <button class="btn btn-primary" (click)="uploadFile()">
          <span>🚀</span> Upload File
        </button>
      </div>
      
      <div class="upload-result" *ngIf="uploadResult">
        <div class="alert" [class.alert-success]="uploadResult.success" [class.alert-error]="!uploadResult.success">
          <div class="alert-header">
            <span>{{ uploadResult.success ? '✅' : '❌' }}</span>
            <strong>{{ uploadResult.message }}</strong>
          </div>
          
          <div *ngIf="uploadResult.success" class="result-stats">
            <div class="stat">
              <span class="stat-value">{{ uploadResult.processedRows }}</span>
              <span class="stat-label">Rows Processed</span>
            </div>
          </div>
          
          <div *ngIf="uploadResult.errors && uploadResult.errors.length > 0" class="error-list">
            <h4>Errors:</h4>
            <ul>
              <li *ngFor="let error of uploadResult.errors">{{ error }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .upload-container {
      padding: 32px;
      margin-bottom: 32px;
    }

    .upload-header {
      text-align: center;
      margin-bottom: 32px;
    }

    .upload-header h2 {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 8px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-purple));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .upload-header p {
      color: var(--text-secondary);
      font-size: 16px;
    }

    .upload-zone {
      border: 2px dashed var(--border-color);
      border-radius: 16px;
      padding: 48px 32px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s ease;
      background: var(--bg-secondary);
      position: relative;
      overflow: hidden;
    }

    .upload-zone::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
      transition: left 0.5s ease;
    }

    .upload-zone:hover {
      border-color: var(--accent-primary);
      background: var(--bg-hover);
      transform: translateY(-2px);
    }

    .upload-zone:hover::before {
      left: 100%;
    }

    .upload-zone.dragover {
      border-color: var(--accent-success);
      background: rgba(16, 185, 129, 0.1);
      transform: scale(1.02);
    }

    .upload-zone.uploading {
      border-color: var(--accent-warning);
      background: rgba(245, 158, 11, 0.1);
    }

    .upload-content {
      position: relative;
      z-index: 1;
    }

    .upload-icon {
      font-size: 48px;
      margin-bottom: 16px;
      display: inline-block;
    }

    .upload-text {
      font-size: 18px;
      color: var(--text-primary);
      margin-bottom: 8px;
    }

    .upload-text strong {
      color: var(--accent-primary);
    }

    .file-info {
      color: var(--text-muted);
      font-size: 14px;
    }

    .upload-actions {
      display: flex;
      justify-content: center;
      gap: 16px;
      margin-top: 24px;
    }

    .upload-result {
      margin-top: 24px;
    }

    .alert {
      padding: 20px;
      border-radius: 12px;
      border: 1px solid;
    }

    .alert-success {
      background: rgba(16, 185, 129, 0.1);
      border-color: var(--accent-success);
      color: var(--accent-success);
    }

    .alert-error {
      background: rgba(239, 68, 68, 0.1);
      border-color: var(--accent-error);
      color: var(--accent-error);
    }

    .alert-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }

    .result-stats {
      display: flex;
      justify-content: center;
      gap: 32px;
      margin: 16px 0;
    }

    .stat {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 32px;
      font-weight: 700;
      color: var(--accent-success);
    }

    .stat-label {
      font-size: 14px;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .error-list {
      margin-top: 16px;
    }

    .error-list h4 {
      margin-bottom: 8px;
      font-size: 16px;
    }

    .error-list ul {
      list-style: none;
      padding: 0;
    }

    .error-list li {
      padding: 8px 0;
      border-bottom: 1px solid rgba(239, 68, 68, 0.2);
    }

    .error-list li:last-child {
      border-bottom: none;
    }

    @media (max-width: 768px) {
      .upload-container {
        padding: 24px;
      }

      .upload-zone {
        padding: 32px 16px;
      }

      .upload-header h2 {
        font-size: 24px;
      }

      .result-stats {
        flex-direction: column;
        gap: 16px;
      }
    }
  `]
})
export class FileUploadComponent {
  @Output() uploadComplete = new EventEmitter<UploadResponse>();

  selectedFile: File | null = null;
  isDragOver = false;
  isUploading = false;
  uploadProgress = 0;
  uploadResult: UploadResponse | null = null;

  constructor(private excelService: ExcelService) {}

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
    if (this.isValidExcelFile(file)) {
      this.selectedFile = file;
      this.uploadResult = null;
    } else {
      this.showError('Please select a valid Excel file (.xlsx or .xls)');
    }
  }

  private isValidExcelFile(file: File): boolean {
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel'
    ];
    return validTypes.includes(file.type) || file.name.match(/\.(xlsx|xls)$/i) !== null;
  }

  uploadFile() {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.uploadProgress = 0;
    this.uploadResult = null;

    // Simulate progress
    const progressInterval = setInterval(() => {
      this.uploadProgress += Math.random() * 30;
      if (this.uploadProgress >= 90) {
        clearInterval(progressInterval);
      }
    }, 200);

    this.excelService.uploadExcel(this.selectedFile).subscribe({
      next: (response) => {
        clearInterval(progressInterval);
        this.uploadProgress = 100;
        this.isUploading = false;
        this.uploadResult = response;
        this.uploadComplete.emit(response);
        
        if (response.success) {
          setTimeout(() => {
            this.clearFile();
          }, 3000);
        }
      },
      error: (error) => {
        clearInterval(progressInterval);
        this.isUploading = false;
        this.showError('Upload failed: ' + (error.error?.message || error.message));
      }
    });
  }

  clearFile() {
    this.selectedFile = null;
    this.uploadResult = null;
    this.uploadProgress = 0;
  }

  private showError(message: string) {
    this.uploadResult = {
      success: false,
      message: message,
      processedRows: 0
    };
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getFileType(filename: string): string {
    const extension = filename.split('.').pop()?.toUpperCase();
    return extension || 'Unknown';
  }
}