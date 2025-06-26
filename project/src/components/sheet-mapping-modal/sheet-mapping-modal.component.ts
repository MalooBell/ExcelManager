// CHEMIN : project/src/components/sheet-mapping-modal/sheet-mapping-modal.component.ts
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SheetMapping } from '../../models/sheet-mapping.model';
import { SheetMappingTemplate, NewSheetMappingTemplate } from '../../models/sheet-mapping-template.model';
import { SheetMappingService } from '../../services/sheet-mapping.service';
import { SheetMappingTemplateService } from '../../services/sheet-mapping-template.service';

@Component({
  selector: 'app-sheet-mapping-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal" style="width: 90vw; max-width: 1200px;">
        <div class="modal-header">
          <h3 class="modal-title">Configurer le Mapping de la Feuille</h3>
          <button class="modal-close" (click)="close()">×</button>
        </div>

        <div class="modal-body" style="max-height: calc(90vh - 200px); overflow-y: auto;">
          <div *ngIf="loading" class="text-center py-8">
            <div class="loading-spinner"></div>
            <p class="text-gray-600 mt-2">Chargement de la configuration...</p>
          </div>

          <div *ngIf="!loading" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-1 space-y-6">
              
              <div class="card">
                <h4 class="card-title mb-4">Appliquer un Modèle</h4>
                <div class="form-group">
                  <label class="form-label">Modèles existants</label>
                  <select class="form-control" [(ngModel)]="selectedTemplateId">
                    <option [ngValue]="null">-- Sélectionner un modèle --</option>
                    <option *ngFor="let t of templates" [value]="t.id">{{ t.name }}</option>
                  </select>
                </div>
                <button class="btn btn-primary w-full" (click)="applyTemplate()" [disabled]="!selectedTemplateId">
                  Appliquer ce modèle
                </button>
              </div>

              <div class="card">
                <h4 class="card-title mb-4">Sauvegarder en tant que Modèle</h4>
                <div class="form-group">
                    <label class="form-label">Nom du nouveau modèle</label>
                    <input type="text" class="form-control" [(ngModel)]="newTemplateName" placeholder="Ex: Rapport Ventes Mensuel">
                </div>
                 <div class="form-group flex items-center">
                    <input type="checkbox" id="ignoreUnmapped" class="mr-3" [(ngModel)]="currentMapping.ignoreUnmapped">
                    <label for="ignoreUnmapped" class="form-label mb-0">Ignorer les colonnes non mappées</label>
                </div>
                <button class="btn btn-success w-full" (click)="saveAsTemplate()" [disabled]="!newTemplateName.trim() || savingTemplate">
                    <div *ngIf="savingTemplate" class="loading-spinner"></div>
                    Sauvegarder comme nouveau modèle
                </button>
              </div>

            </div>

            <div class="lg:col-span-2">
                <div class="flex justify-between items-center mb-4">
                    <h4 class="text-lg font-semibold">Règles de Mapping</h4>
                    <button class="btn btn-secondary btn-sm" (click)="autoFillDestinations()">
                        Remplir les destinations (camelCase)
                    </button>
                </div>
                <div class="table-container card p-0">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Colonne Source (depuis Excel)</th>
                                <th>Champ de Destination (dans la base)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr *ngFor="let rule of currentMapping.mappings">
                                <td class="font-medium text-gray-700">{{ rule.source }}</td>
                                <td>
                                    <input type="text" class="form-control form-control-sm" [(ngModel)]="rule.destination" placeholder="Nom du champ...">
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" (click)="close()">
            Annuler
          </button>
          <button type="button" class="btn btn-primary" (click)="save()" [disabled]="saving">
            <div *ngIf="saving" class="loading-spinner"></div>
            Sauvegarder le Mapping pour CETTE Feuille
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .space-y-6 > * + * { margin-top: 1.5rem; }
    .lg\\:col-span-1 { grid-column: span 1 / span 1; }
    .lg\\:col-span-2 { grid-column: span 2 / span 2; }
    .p-0 { padding: 0; }
  `]
})
export class SheetMappingModalComponent implements OnInit {
  @Input() sheetId!: number;
  @Input() sourceColumns: string[] = [];
  @Output() closeModal = new EventEmitter<void>();

  loading = true;
  saving = false;
  savingTemplate = false;

  currentMapping: SheetMapping = { mappings: [], ignoreUnmapped: false };
  templates: SheetMappingTemplate[] = [];
  selectedTemplateId: number | null = null;
  newTemplateName = '';

  constructor(
    private mappingService: SheetMappingService,
    private templateService: SheetMappingTemplateService
  ) {}

  ngOnInit() {
    this.loadInitialData();
  }

  loadInitialData() {
    this.loading = true;
    // On charge en parallèle le mapping actuel et la liste des modèles.
    Promise.all([
      this.mappingService.getMapping(this.sheetId).toPromise(),
      this.templateService.getAll().toPromise()
    ]).then(([mapping, templates]) => {
      this.templates = templates || [];
      if (mapping) {
        this.currentMapping = mapping;
        this.sourceColumns.forEach(col => {
            if (!this.currentMapping.mappings.some(m => m.source === col)) {
                this.currentMapping.mappings.push({ source: col, destination: '' });
            }
        });
      } else {
        this.resetToDefault();
      }
      this.loading = false;
    }).catch(error => {
      console.error("Failed to load initial data", error);
      this.loading = false;
      this.resetToDefault();
    });
  }

  resetToDefault() {
    this.currentMapping = {
      mappings: this.sourceColumns.map(col => ({ source: col, destination: '' })),
      ignoreUnmapped: false
    };
  }

  autoFillDestinations() {
    this.currentMapping.mappings.forEach(rule => {
        if (!rule.destination) { // Ne remplit que si c'est vide
            rule.destination = this.toCamelCase(rule.source);
        }
    });
  }

  applyTemplate() {
    if (!this.selectedTemplateId) return;
    this.mappingService.applyTemplate(this.sheetId, this.selectedTemplateId).subscribe(updatedMapping => {
      this.currentMapping = updatedMapping;
      alert('Modèle appliqué avec succès !');
    });
  }

  saveAsTemplate() {
    if (!this.newTemplateName.trim()) {
        alert('Veuillez donner un nom à votre modèle.');
        return;
    }
    this.savingTemplate = true;
    const newTemplate: NewSheetMappingTemplate = {
        name: this.newTemplateName,
        mappingDefinition: this.currentMapping // Le service se chargera de sérialiser
    };
    this.templateService.create(newTemplate).subscribe({
      next: (createdTemplate) => {
        this.templates.push(createdTemplate);
        this.selectedTemplateId = createdTemplate.id;
        this.newTemplateName = '';
        this.savingTemplate = false;
        alert('Modèle sauvegardé avec succès !');
      },
      error: (err) => {
        console.error("Failed to save template", err);
        alert("Erreur lors de la sauvegarde du modèle. Le nom existe peut-être déjà.");
        this.savingTemplate = false;
      }
    });
  }

  save() {
    this.saving = true;
    this.mappingService.saveMapping(this.sheetId, this.currentMapping).subscribe({
      next: () => {
        this.saving = false;
        alert('Mapping pour cette feuille sauvegardé !');
        this.close();
      },
      error: (err) => {
        console.error('Failed to save mapping', err);
        this.saving = false;
      }
    });
  }
  
  // Fonctions utilitaires
  toCamelCase = (s: string) => s.replace(/[^a-zA-Z0-9]+(.)?/g, (m, c) => c ? c.toUpperCase() : '').replace(/^\w/, c => c.toLowerCase());
  onOverlayClick = (e: MouseEvent) => { if (e.target === e.currentTarget) this.close(); };
  close = () => this.closeModal.emit();
}