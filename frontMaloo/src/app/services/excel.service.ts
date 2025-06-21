import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RowEntity, UploadResponse, ModificationHistory, PageResponse } from '../models/row-entity.model';

@Injectable({
  providedIn: 'root'
})
export class ExcelService {
  private readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Upload Excel file
  uploadExcel(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadResponse>(`${this.baseUrl}/excel/upload`, formData);
  }

  // Get all rows with pagination and search
  getRows(page: number = 0, size: number = 10, fileName?: string, keyword?: string): Observable<PageResponse<RowEntity>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (fileName) params = params.set('fileName', fileName);
    if (keyword) params = params.set('keyword', keyword);

    return this.http.get<PageResponse<RowEntity>>(`${this.baseUrl}/rows`, { params });
  }

  // Get row by ID
  getRowById(id: number): Observable<RowEntity> {
    return this.http.get<RowEntity>(`${this.baseUrl}/rows/${id}`);
  }

  // Create new row
  createRow(row: RowEntity): Observable<RowEntity> {
    return this.http.post<RowEntity>(`${this.baseUrl}/rows`, row);
  }

  // Update row
  updateRow(id: number, row: RowEntity): Observable<RowEntity> {
    return this.http.put<RowEntity>(`${this.baseUrl}/rows/${id}`, row);
  }

  // Delete row
  deleteRow(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rows/${id}`);
  }

  // Download filtered data
  downloadExcel(fileName?: string, keyword?: string): Observable<Blob> {
    let params = new HttpParams();
    if (fileName) params = params.set('fileName', fileName);
    if (keyword) params = params.set('keyword', keyword);

    return this.http.get(`${this.baseUrl}/rows/download`, {
      params,
      responseType: 'blob'
    });
  }

  // Get modification history
  getHistory(rowId: number): Observable<ModificationHistory[]> {
    return this.http.get<ModificationHistory[]>(`${this.baseUrl}/history/row/${rowId}`);
  }

  // Reset all data
  resetAll(): Observable<string> {
    return this.http.delete<string>(`${this.baseUrl}/reset`);
  }

  getAllHistory(): Observable<ModificationHistory[]> {
    return this.http.get<ModificationHistory[]>(`${this.baseUrl}/history/all`);
  }
}