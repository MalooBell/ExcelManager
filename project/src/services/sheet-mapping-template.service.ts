// CHEMIN : project/src/services/sheet-mapping-template.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { SheetMappingTemplate, NewSheetMappingTemplate } from '../models/sheet-mapping-template.model';

@Injectable({
  providedIn: 'root'
})
export class SheetMappingTemplateService {

  private baseUrl = '/mappings/templates';

  constructor(private api: ApiService) {}

  getAll(): Observable<SheetMappingTemplate[]> {
    return this.api.get<SheetMappingTemplate[]>(this.baseUrl);
  }

  create(template: NewSheetMappingTemplate): Observable<SheetMappingTemplate> {
    return this.api.post<SheetMappingTemplate>(this.baseUrl, template);
  }
}