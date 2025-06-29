// CHEMIN: project/src/app/app.routes.ts
import { Routes } from '@angular/router';
import { DashboardComponent } from '../pages/dashboard/dashboard.component';
import { FileProcessingComponent } from '../pages/file-processing/file-processing.component';
import { LoginComponent } from '../pages/login/login.component'; // Importer le nouveau composant
import { authGuard } from '../services/auth.guard'; // Importer le nouveau guard
import { AdminComponent } from '../pages/admin/admin.component';

export const routes: Routes = [
  // Routes protégées
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'file/:id', component: FileProcessingComponent, canActivate: [authGuard] },
   { path: 'admin', component: AdminComponent, canActivate: [authGuard] }, 
  // Route publique
  { path: 'login', component: LoginComponent },

  // Redirection par défaut
  { path: '**', redirectTo: '' }
];