// CHEMIN : project/src/components/graph-modal/graph-modal.component.ts
import { Component, Input, Output, EventEmitter, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { GraphService } from '../../services/graph.service';
import { GraphRequest, GraphData } from '../../models/graph.model';
import { RowEntity } from '../../models/row.model';

Chart.register(...registerables);

@Component({
  selector: 'app-graph-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal" style="width: 95vw; max-width: 1200px; max-height: 90vh;">
        <div class="modal-header">
          <h3 class="modal-title">Générer un graphique</h3>
          <button class="modal-close" (click)="close()">×</button>
        </div>

        <div class="modal-body" style="max-height: calc(90vh - 200px); overflow-y: auto;">
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            <div class="lg:col-span-1">
              <div class="card">
                <h4 class="text-lg font-semibold mb-4">Configuration du graphique</h4>
                
                <div class="form-group">
                  <label class="form-label">Type de graphique</label>
                  <select class="form-control" [(ngModel)]="graphRequest.chartType" (change)="onConfigChange()">
                    <option value="pie">Graphique en secteurs</option>
                    <option value="bar">Graphique en barres</option>
                  </select>
                </div>

                <div class="form-group" *ngIf="graphRequest.chartType === 'bar'">
                    <label class="form-label">Opération d'agrégation</label>
                    <select class="form-control" [(ngModel)]="graphRequest.aggregationType">
                        <option value="COUNT">Compter les occurrences</option>
                        <option value="SUM">Sommer les valeurs (numériques)</option>
                    </select>
                </div>

                <div class="form-group">
                  <label class="form-label">Colonne catégorie (Axe X)</label>
                  <select class="form-control" [(ngModel)]="graphRequest.categoryColumn" (change)="onConfigChange()">
                    <option value="">Sélectionnez une colonne</option>
                    <option *ngFor="let column of columns" [value]="column">{{ column }}</option>
                  </select>
                </div>

                <div class="form-group" *ngIf="graphRequest.chartType === 'bar' && graphRequest.aggregationType === 'SUM'">
                  <label class="form-label">Colonnes valeurs (Axe Y)</label>
                  <div class="space-y-2">
                    <div *ngFor="let column of numericColumns" class="flex items-center">
                      <input 
                        type="checkbox" 
                        [id]="'col-' + column"
                        [checked]="graphRequest.valueColumns.includes(column)"
                        (change)="onValueColumnChange(column, $event)"
                        class="mr-2">
                      <label [for]="'col-' + column" class="text-sm">{{ column }}</label>
                    </div>
                  </div>
                </div>

                <div class="form-group" *ngIf="graphRequest.chartType === 'bar'">
                    <label class="form-label">Grouper par (Optionnel)</label>
                    <select class="form-control" [(ngModel)]="graphRequest.groupingColumn">
                        <option value="">-- Aucun groupage --</option>
                        <option *ngFor="let column of columns" [value]="column">{{ column }}</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">Nombre de résultats à afficher</label>
                    <select class="form-control" [(ngModel)]="graphRequest.limit">
                        <option [ngValue]="10">Top 10</option>
                        <option [ngValue]="20">Top 20</option>
                        <option [ngValue]="50">Top 50</option>
                        <option [ngValue]="null">Tout afficher (lent)</option>
                    </select>
                </div>

                <button 
                  class="btn btn-primary w-full"
                  (click)="generateGraph()"
                  [disabled]="!canGenerateGraph() || loading">
                  <div *ngIf="loading" class="loading-spinner"></div>
                  Générer le graphique
                </button>
              </div>
            </div>

            <div class="lg:col-span-2">
              <div class="card">
                <div class="flex justify-between items-center mb-4">
                  <h4 class="text-lg font-semibold">Aperçu du graphique</h4>
                  <button 
                    *ngIf="chartData" 
                    class="btn btn-success btn-sm"
                    (click)="downloadChart()">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                            d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    Télécharger
                  </button>
                </div>
                
                <div class="chart-container" style="position: relative; height: 400px;">
                  <canvas #chartCanvas></canvas>
                  <div *ngIf="!chartData" class="absolute inset-0 flex items-center justify-center">
                    <p class="text-gray-500">Configurez les paramètres et cliquez sur "Générer le graphique"</p>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" (click)="close()">
            Fermer
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .grid {
      display: grid;
    }
    .grid-cols-1 {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
    .gap-6 {
      gap: 1.5rem;
    }
    .space-y-2 > * + * {
      margin-top: 0.5rem;
    }
    .mr-2 {
      margin-right: 0.5rem;
    }
    .absolute {
      position: absolute;
    }
    .inset-0 {
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
    }
    .w-4 { width: 1rem; }
    .h-4 { height: 1rem; }

    @media (min-width: 1024px) {
      .lg\\:grid-cols-3 {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
      .lg\\:col-span-1 {
        grid-column: span 1 / span 1;
      }
      .lg\\:col-span-2 {
        grid-column: span 2 / span 2;
      }
    }

    .chart-container {
      position: relative;
      width: 100%;
      height: 400px;
    }
  `]
})
export class GraphModalComponent implements OnInit {
  @Input() sheetId!: number; 
  @Input() columns: string[] = [];
  @Input() sampleRows: RowEntity[] = [];
  @Output() closeModal = new EventEmitter<void>();
  @ViewChild('chartCanvas', { static: true }) chartCanvas!: ElementRef<HTMLCanvasElement>;

  graphRequest: GraphRequest = {
    chartType: 'pie',
    categoryColumn: '',
    valueColumns: [],
    aggregationType: 'COUNT',
    groupingColumn: '',
    limit: 20
  };

  chartData: GraphData | null = null;
  loading = false;
  chart: Chart | null = null;
  numericColumns: string[] = [];

  constructor(private graphService: GraphService) {}

  ngOnInit() {
    if (this.columns.length > 0) {
      this.graphRequest.categoryColumn = this.columns[0];
    }
    this.detectNumericColumns();
  }

  onOverlayClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  generateGraph() {
    if (!this.canGenerateGraph()) return;
    this.loading = true;
    
    this.graphService.generateGraph(this.sheetId, this.graphRequest).subscribe({
      next: (data: GraphData) => {
        this.chartData = data;
        this.renderChart();
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors de la génération du graphique:', error);
        this.loading = false;
      }
    });
  }
  
  /**
   * MODIFIÉ : Ajout d'une conversion de type explicite `(row.data as any)`
   * pour indiquer au compilateur que nous accédons à une propriété dynamique,
   * ce qui résout les erreurs "Element implicitly has an 'any' type".
   */
  private detectNumericColumns() {
      if (this.sampleRows.length === 0) {
          this.numericColumns = [...this.columns];
          return;
      }
      this.numericColumns = this.columns.filter(column => {
          const firstRowWithValue = this.sampleRows.find(row => (row.data as any)[column] != null && (row.data as any)[column] !== '');
          if (!firstRowWithValue) return false;
          const sampleValue = (firstRowWithValue.data as any)[column];
          return !isNaN(parseFloat(String(sampleValue).replace(',', '.')));
      });
  }

  close() {
    if (this.chart) this.chart.destroy();
    this.closeModal.emit();
  }

  onConfigChange() {
    if (this.graphRequest.chartType === 'pie') {
      this.graphRequest.valueColumns = [];
      this.graphRequest.aggregationType = 'COUNT';
    } else if (!this.graphRequest.aggregationType) {
      this.graphRequest.aggregationType = 'COUNT';
    }
  }

  onValueColumnChange(column: string, event: any) {
    const isChecked = event.target.checked;
    if (isChecked) {
      if (!this.graphRequest.valueColumns.includes(column)) {
        this.graphRequest.valueColumns.push(column);
      }
    } else {
      this.graphRequest.valueColumns = this.graphRequest.valueColumns.filter(c => c !== column);
    }
  }

  canGenerateGraph(): boolean {
    if (!this.graphRequest.categoryColumn) return false;
    if (this.graphRequest.aggregationType === 'COUNT') return true;
    return this.graphRequest.valueColumns.length > 0;
  }
  
  private renderChart() {
    if (!this.chartData) return;
    if (this.chart) this.chart.destroy();
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;
    const config: ChartConfiguration = {
      type: this.graphRequest.chartType as ChartType,
      data: {
        labels: this.chartData.labels,
        datasets: this.chartData.datasets.map((dataset, index) => ({
          ...dataset,
          backgroundColor: this.getBackgroundColors(index, this.chartData?.labels.length || 1),
          borderColor: this.getBorderColors(index, this.chartData?.labels.length || 1),
          borderWidth: 1
        }))
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { position: 'top' }, title: { display: true, text: `Graphique ${this.graphRequest.chartType}` } },
        scales: this.graphRequest.chartType === 'pie' ? undefined : { y: { beginAtZero: true } }
      }
    };
    this.chart = new Chart(ctx, config);
  }

  private getBackgroundColors(datasetIndex: number, dataLength: number): string[] {
    const colors = ['rgba(30, 58, 138, 0.8)', 'rgba(5, 150, 105, 0.8)', 'rgba(59, 130, 246, 0.8)', 'rgba(16, 185, 129, 0.8)', 'rgba(99, 102, 241, 0.8)', 'rgba(245, 158, 11, 0.8)', 'rgba(239, 68, 68, 0.8)', 'rgba(168, 85, 247, 0.8)'];
    if (this.graphRequest.chartType === 'pie') return colors.slice(0, dataLength);
    return [colors[datasetIndex % colors.length]];
  }
  private getBorderColors(datasetIndex: number, dataLength: number): string[] {
    const colors = ['rgba(30, 58, 138, 1)', 'rgba(5, 150, 105, 1)', 'rgba(59, 130, 246, 1)', 'rgba(16, 185, 129, 1)', 'rgba(99, 102, 241, 1)', 'rgba(245, 158, 11, 1)', 'rgba(239, 68, 68, 1)', 'rgba(168, 85, 247, 1)'];
    if (this.graphRequest.chartType === 'pie') return colors.slice(0, dataLength);
    return [colors[datasetIndex % colors.length]];
  }

  downloadChart() {
    if (!this.chart) return;
    const link = document.createElement('a');
    link.download = `graphique-${this.graphRequest.chartType}-${Date.now()}.png`;
    link.href = this.chart.toBase64Image();
    link.click();
  }
}