import { Routes } from '@angular/router';
import { DashboardComponent } from '../pages/dashboard/dashboard.component';
import { FileProcessingComponent } from '../pages/file-processing/file-processing.component';
import { FileValidationComponent } from '../pages/file-validation/file-validation.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'file/:id', component: FileProcessingComponent },
  { path: 'validate/:id', component: FileValidationComponent },
  { path: '**', redirectTo: '' }
];