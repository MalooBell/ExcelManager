// CHEMIN: project/src/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

// L'interface pour la réponse de l'API
export interface AuthResponse {
  token: string;
  username: string;
  role: 'USER' | 'ADMIN';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8081/api/auth';
  private tokenKey = 'auth_token';
  private userKey = 'auth_user'; // Clé pour stocker les infos utilisateur

  constructor(private http: HttpClient, private router: Router) { }

  login(credentials: { username: string, password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials).pipe(
      tap(response => this.storeAuthInfo(response))
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey); // Ne pas oublier de supprimer les infos utilisateur
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  // --- LA MÉTHODE MANQUANTE ---
  /**
   * Vérifie si l'utilisateur actuellement connecté a le rôle ADMIN.
   */
  isAdmin(): boolean {
    const user = this.getUser();
    return user?.role === 'ADMIN';
  }

  // --- NOUVELLES MÉTHODES UTILITAIRES ---
  /**
   * Récupère les informations de l'utilisateur depuis le localStorage.
   */
  getUser(): { username: string, role: string } | null {
    const userStr = localStorage.getItem(this.userKey);
    if (userStr) {
      return JSON.parse(userStr);
    }
    return null;
  }

  /**
   * Stocke le token et les informations de l'utilisateur dans le localStorage.
   */
  private storeAuthInfo(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    const userInfo = { username: response.username, role: response.role };
    localStorage.setItem(this.userKey, JSON.stringify(userInfo));
  }
}