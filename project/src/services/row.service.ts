import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { RowEntity, ModificationHistory } from '../models/row.model';
import { PageResponse } from '../models/file.model';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class RowService {
  constructor(private api: ApiService) {}

  getRowsForFile(
    fileId: number,
    page: number = 0,
    size: number = 50,
    keyword?: string,
    sort?: string
  ): Observable<PageResponse<RowEntity>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (keyword) params = params.set('keyword', keyword);
    if (sort) params = params.set('sort', sort);
    
    return this.api.get<PageResponse<RowEntity>>(`/rows/file/${fileId}`, params);
  }

  searchRows(
    fileName?: string,
    keyword?: string,
    page: number = 0,
    size: number = 50
  ): Observable<PageResponse<RowEntity>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (fileName) params = params.set('fileName', fileName);
    if (keyword) params = params.set('keyword', keyword);
    
    return this.api.get<PageResponse<RowEntity>>('/rows', params);
  }

  getRow(id: number): Observable<RowEntity> {
    return this.api.get<RowEntity>(`/rows/${id}`);
  }

  createRow(fileId: number, row: Partial<RowEntity>): Observable<RowEntity> {
    return this.api.post<RowEntity>(`/rows/file/${fileId}`, row);
  }

  updateRow(id: number, row: Partial<RowEntity>): Observable<RowEntity> {
    return this.api.put<RowEntity>(`/rows/${id}`, row);
  }

  deleteRow(id: number): Observable<void> {
    return this.api.delete<void>(`/rows/${id}`);
  }

  getRowHistory(rowId: number): Observable<ModificationHistory[]> {
    return this.api.get<ModificationHistory[]>(`/history/row/${rowId}`);
  }

  getAllHistory(): Observable<ModificationHistory[]> {
    return this.api.get<ModificationHistory[]>('/history/all');
  }
}