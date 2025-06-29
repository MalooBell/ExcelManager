// CHEMIN: project/src/pages/login/login.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex items-center justify-center min-h-screen bg-gray-100">
      <div class="w-full max-w-md p-8 space-y-6 bg-white rounded-lg shadow-md">
        <h2 class="text-2xl font-bold text-center text-gray-900">Connexion</h2>
        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="username" class="form-label">Nom d'utilisateur</label>
            <input id="username" name="username" type="text" [(ngModel)]="credentials.username" required class="form-control">
          </div>
          <div class="form-group">
            <label for="password" class="form-label">Mot de passe</label>
            <input id="password" name="password" type="password" [(ngModel)]="credentials.password" required class="form-control">
          </div>
           <p *ngIf="errorMessage" class="text-red-500 text-sm text-center mb-4">{{ errorMessage }}</p>
          <div>
            <button type="submit" class="btn btn-primary w-full">
              Se connecter
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class LoginComponent {
  credentials = { username: '', password: '' };
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.router.navigate(['/']); // Redirige vers le tableau de bord après succès
      },
      error: (err) => {
        this.errorMessage = 'Nom d\'utilisateur ou mot de passe incorrect.';
        console.error('Erreur de connexion', err);
      }
    });
  }
}