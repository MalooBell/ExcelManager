import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { FileEntity, UploadResponse, PageResponse } from '../models/file.model';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  constructor(private api: ApiService) {}

  uploadFile(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.postFormData<UploadResponse>('/excel/upload', formData);
  }

  getFiles(page: number = 0, size: number = 10, search?: string): Observable<PageResponse<FileEntity>> { // MODIFICATION : ajout de 'search'
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'uploadTimestamp,desc');

    if (search) { // AJOUT DE CETTE CONDITION
      params = params.set('search', search);
    }
      
    return this.api.get<PageResponse<FileEntity>>('/files', params);
  }

  getFile(id: number): Observable<FileEntity> {
    return this.api.get<FileEntity>(`/files/${id}`);
  }

  deleteFile(id: number): Observable<void> {
    return this.api.delete<void>(`/files/${id}`);
  }

  downloadFile(fileName?: string, keyword?: string): Observable<Blob> {
    let params = new HttpParams();
    if (fileName) params = params.set('fileName', fileName);
    if (keyword) params = params.set('keyword', keyword);
    return this.api.downloadFile('/rows/download', params);
  }
}