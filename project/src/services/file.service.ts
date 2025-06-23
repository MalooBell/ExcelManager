import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { FileEntity, UploadResponse, PageResponse } from '../models/file.model';
import { SheetEntity } from '../models/sheet.model';
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

  getFiles(page: number = 0, size: number = 10, search?: string): Observable<PageResponse<FileEntity>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'uploadTimestamp,desc');

    if (search) {
      params = params.set('search', search);
    }
      
    return this.api.get<PageResponse<FileEntity>>('/files', params);
  }

  getFile(id: number): Observable<FileEntity> {
    return this.api.get<FileEntity>(`/files/${id}`);
  }

  getSheets(fileId: number): Observable<SheetEntity[]> {
    return this.api.get<SheetEntity[]>(`/files/${fileId}/sheets`);
  }

  deleteFile(id: number): Observable<void> {
    return this.api.delete<void>(`/files/${id}`);
  }

  downloadSheet(sheetId: number, fileName: string, keyword?: string): Observable<Blob> {
    let params = new HttpParams();
    if (keyword) params = params.set('keyword', keyword);
    return this.api.downloadFile(`/rows/sheet/${sheetId}/download`, params);
  }
}