import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { GraphRequest, GraphData } from '../models/graph.model';

@Injectable({
  providedIn: 'root'
})
export class GraphService {
  constructor(private api: ApiService) {}

  generateGraph(sheetId: number, request: GraphRequest): Observable<GraphData> {
    return this.api.post<GraphData>(`/graphs/sheet/${sheetId}`, request);
  }
}