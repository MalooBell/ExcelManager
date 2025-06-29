import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User { id: number; username: string; email: string; role: 'USER' | 'ADMIN'; }
export interface AuditLog { id: number; username: string; action: string; details: string; timestamp: string; }

@Injectable({ providedIn: 'root' })
export class AdminService {
  private baseUrl = 'http://localhost:8081/api/admin';

  constructor(private http: HttpClient) { }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/users`);
  }

  getAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/audits`);
  }
}