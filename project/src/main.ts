import { Component } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { routes } from './app/app.routes';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div class="min-h-screen bg-gray-50">
      <header class="bg-white shadow-sm border-b border-gray-200">
        <div class="container">
          <div class="flex items-center justify-between py-4">
            <div class="flex items-center">
              <div class="w-8 h-8 bg-primary-blue rounded-lg flex items-center justify-center mr-3">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                </svg>
              </div>
              <div>
                <h1 class="text-xl font-bold text-gray-900">Excel Manager</h1>
                <p class="text-sm text-gray-500">Powered by MBLM</p>
              </div>
            </div>
            <div class="hidden sm:flex items-center space-x-4">
              <div class="flex items-center text-sm text-gray-600">
                <div class="w-3 h-3 bg-accent-green rounded-full mr-2"></div>
                Système opérationnel
              </div>
            </div>
          </div>
        </div>
      </header>

      <main>
        <router-outlet></router-outlet>
      </main>

      <footer class="bg-white border-t border-gray-200 mt-16">
        <div class="container py-8">
          <div class="flex flex-col md:flex-row justify-between items-center">
            <div class="text-gray-600 text-sm mb-4 md:mb-0">
              © 2024 MALOO BELL Cameroun. Tous droits réservés.
            </div>
            <div class="flex items-center space-x-6">
              <div class="flex items-center text-sm text-gray-600">
                <div class="w-2 h-2 bg-primary-blue rounded-full mr-2"></div>
                Version 1.0.0
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    .min-h-screen { min-height: 100vh; }
    .w-8 { width: 2rem; }
    .h-8 { height: 2rem; }
    .w-5 { width: 1.25rem; }
    .h-5 { height: 1.25rem; }
    .w-3 { width: 0.75rem; }
    .h-3 { height: 0.75rem; }
    .w-2 { width: 0.5rem; }
    .h-2 { height: 0.5rem; }
    .mr-3 { margin-right: 0.75rem; }
    .mr-2 { margin-right: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-16 { margin-top: 4rem; }
    .space-x-4 > * + * { margin-left: 1rem; }
    .space-x-6 > * + * { margin-left: 1.5rem; }
    .bg-primary-blue { background-color: var(--primary-blue); }
    .bg-accent-green { background-color: var(--accent-green); }
    .text-white { color: var(--white); }
    .hidden { display: none; }
    .border-t { border-top: 1px solid var(--gray-200); }
    .border-b { border-bottom: 1px solid var(--gray-200); }
    
    @media (min-width: 640px) {
      .sm\\:flex { display: flex; }
    }
    
    @media (min-width: 768px) {
      .md\\:flex-row { flex-direction: row; }
      .md\\:mb-0 { margin-bottom: 0; }
    }
  `]
})
export class App {}

bootstrapApplication(App, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi())
  ]
});