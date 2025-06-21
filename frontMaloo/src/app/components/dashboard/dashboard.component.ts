import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadComponent } from '../file-upload/file-upload.component';
import { DataTableComponent } from '../data-table/data-table.component';
import { UploadResponse } from '../../models/row-entity.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FileUploadComponent, DataTableComponent],
  template: `
    <div class="dashboard">
      <!-- Header -->
      <header class="dashboard-header">
        <div class="container">
          <div class="header-content">
            <div class="logo-section">
              <h1 class="logo">📊 Excel Manager</h1>
              <p class="tagline">Dynamic Excel Data Management System</p>
            </div>
            <div class="header-stats" *ngIf="lastUploadResult">
              <div class="stat-card">
                <div class="stat-value">{{ lastUploadResult.processedRows }}</div>
                <div class="stat-label">Last Upload</div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="dashboard-main">
        <div class="container">
          <!-- Upload Section -->
          <section class="upload-section">
            <app-file-upload (uploadComplete)="onUploadComplete($event)"></app-file-upload>
          </section>

          <!-- Data Section -->
          <section class="data-section">
            <app-data-table [refreshTrigger]="refreshTrigger"></app-data-table>
          </section>
        </div>
      </main>

      <!-- Footer -->
      <footer class="dashboard-footer">
        <div class="container">
          <div class="footer-content">
            <p>&copy; 2024 Excel Manager. Built with Angular & Spring Boot.</p>
            <div class="footer-links">
              <a href="#" class="footer-link">Documentation</a>
              <a href="#" class="footer-link">API Reference</a>
              <a href="#" class="footer-link">Support</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    .dashboard {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .dashboard-header {
      background: linear-gradient(135deg, var(--bg-secondary), var(--bg-tertiary));
      border-bottom: 1px solid var(--border-color);
      padding: 32px 0;
      position: sticky;
      top: 0;
      z-index: 100;
      backdrop-filter: blur(10px);
    }

    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 32px;
    }

    .logo-section {
      flex: 1;
    }

    .logo {
      font-size: 36px;
      font-weight: 800;
      margin: 0 0 8px 0;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-purple), var(--accent-pink));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: -1px;
    }

    .tagline {
      color: var(--text-secondary);
      font-size: 16px;
      margin: 0;
      font-weight: 500;
    }

    .header-stats {
      display: flex;
      gap: 16px;
    }

    .stat-card {
      background: var(--bg-card);
      padding: 16px 24px;
      border-radius: 12px;
      border: 1px solid var(--border-color);
      text-align: center;
      min-width: 120px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--accent-success);
      margin-bottom: 4px;
    }

    .stat-label {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .dashboard-main {
      flex: 1;
      padding: 48px 0;
      background: var(--bg-primary);
    }

    .upload-section {
      margin-bottom: 48px;
    }

    .data-section {
      animation: slideUp 0.6s ease-out;
    }

    .dashboard-footer {
      background: var(--bg-secondary);
      border-top: 1px solid var(--border-color);
      padding: 32px 0;
      margin-top: auto;
    }

    .footer-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 24px;
    }

    .footer-content p {
      margin: 0;
      color: var(--text-secondary);
      font-size: 14px;
    }

    .footer-links {
      display: flex;
      gap: 24px;
    }

    .footer-link {
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 14px;
      transition: color 0.2s ease;
    }

    .footer-link:hover {
      color: var(--accent-primary);
    }

    @media (max-width: 768px) {
      .dashboard-header {
        padding: 24px 0;
      }

      .header-content {
        flex-direction: column;
        text-align: center;
        gap: 24px;
      }

      .logo {
        font-size: 28px;
      }

      .tagline {
        font-size: 14px;
      }

      .dashboard-main {
        padding: 32px 0;
      }

      .upload-section {
        margin-bottom: 32px;
      }

      .footer-content {
        flex-direction: column;
        text-align: center;
        gap: 16px;
      }

      .footer-links {
        justify-content: center;
      }
    }

    @media (max-width: 480px) {
      .header-stats {
        flex-direction: column;
        width: 100%;
      }

      .stat-card {
        min-width: unset;
      }
    }
  `]
})
export class DashboardComponent {
  lastUploadResult: UploadResponse | null = null;
  refreshTrigger: any = null;

  onUploadComplete(result: UploadResponse) {
    this.lastUploadResult = result;
    if (result.success) {
      // Trigger data table refresh
      this.refreshTrigger = Date.now();
    }
  }
}