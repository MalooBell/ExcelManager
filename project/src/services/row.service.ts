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

  getRowsForSheet(
    sheetId: number,
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
    
    return this.api.get<PageResponse<RowEntity>>(`/rows/sheet/${sheetId}`, params);
  }

  getRow(id: number): Observable<RowEntity> {
    return this.api.get<RowEntity>(`/rows/${id}`);
  }

  createRow(sheetId: number, row: Partial<RowEntity>): Observable<RowEntity> {
    return this.api.post<RowEntity>(`/rows/sheet/${sheetId}`, row);
  }

  updateRow(id: number, row: Partial<RowEntity>): Observable<RowEntity> {
    return this.api.put<RowEntity>(`/rows/${id}`, row);
  }

  deleteRow(id: number): Observable<void> {
    return this.api.delete<void>(`/rows/${id}`);
  }

  getHistoryForSheet(sheetId: number, page: number, size: number): Observable<PageResponse<ModificationHistory>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.api.get<PageResponse<ModificationHistory>>(`/history/sheet/${sheetId}`, params);
  }

  getRowHistory(rowId: number): Observable<ModificationHistory[]> {
    return this.api.get<ModificationHistory[]>(`/history/row/${rowId}`);
  }

  getAllHistory(): Observable<ModificationHistory[]> {
    return this.api.get<ModificationHistory[]>('/history/all');
  }

  // Nouvelle méthode pour récupérer les détails d'un historique par sheetName
  getHistoryBySheetName(sheetName: string): Observable<ModificationHistory[]> {
    const params = new HttpParams().set('sheetName', sheetName);
    return this.api.get<ModificationHistory[]>('/history/bySheetName', params);
  }
}