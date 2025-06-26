// CHEMIN : project/src/pages/file-validation/file-validation.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FileEntity } from '../../models/file.model';
import { SheetEntity } from '../../models/sheet.model';
import { FileService } from '../../services/file.service';
import { ExcelPreviewService } from '../../services/excel-preview.service';
import { SheetPreviewModalComponent } from '../../components/excel-preview/excel-preview.component';
@Component({
  selector: 'app-file-validation',
  standalone: true,
  imports: [CommonModule, SheetPreviewModalComponent],
  template: `
    <div class="container py-8 page-transition">
      <div *ngIf="loading" class="text-center py-16">
        <div class="loading-spinner"></div>
        <p class="mt-4">Chargement du fichier...</p>
      </div>

      <div *ngIf="!loading && file">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Validation Manuelle Requise</h1>
        <p class="text-gray-600 mb-8">
          Le système n'a pas pu déterminer avec certitude la structure du fichier <strong>{{ file.fileName }}</strong>. 
          Veuillez valider la ligne d'en-tête pour chaque feuille.
        </p>

        <div class="space-y-4">
          <div *ngFor="let sheet of sheets" class="card flex items-center justify-between">
            <div>
              <h3 class="font-bold text-lg">{{ sheet.sheetName }}</h3>
              <span *ngIf="validatedSheets.has(sheet.id)" class="text-green-600 font-semibold">✓ En-tête validé</span>
              <span *ngIf="!validatedSheets.has(sheet.id)" class="text-yellow-600 font-semibold">En attente de validation...</span>
            </div>
            <button class="btn btn-primary" (click)="openPreview(sheet)">
              Valider l'En-tête
            </button>
          </div>
        </div>
        
        <div class="mt-8 text-center">
            <button class="btn btn-success btn-lg" [disabled]="!allSheetsValidated()" (click)="finishValidation()">
                Terminer et Traiter le Fichier
            </button>
        </div>
      </div>
    </div>

    <app-sheet-preview-modal *ngIf="sheetToPreview"
        [fileId]="fileId"
        [sheetId]="sheetToPreview.id"
        [sheetIndex]="sheetToPreview.sheetIndex"
        (closeModal)="sheetToPreview = null"
        (headerSelected)="onHeaderManuallySelected($event)">
    </app-sheet-preview-modal>
  `
})
export class FileValidationComponent implements OnInit {
  loading = true;
  fileId!: number;
  file: FileEntity | null = null;
  sheets: SheetEntity[] = [];
  validatedSheets = new Set<number>();
  sheetToPreview: SheetEntity | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fileService: FileService,
    private previewService: ExcelPreviewService
  ) {}

  ngOnInit() {
    this.fileId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadFileAndSheets();
  }

  loadFileAndSheets() {
    this.loading = true;
    this.fileService.getFile(this.fileId).subscribe(file => {
      this.file = file;
      this.fileService.getSheets(this.fileId).subscribe(sheets => {
        this.sheets = sheets;
        // On suppose qu'aucune feuille n'est validée au début
        this.loading = false;
      });
    });
  }

  openPreview(sheet: SheetEntity) {
    this.sheetToPreview = sheet;
  }

  onHeaderManuallySelected(headerRowIndex: number) {
    if (!this.sheetToPreview) return;
    const sheetId = this.sheetToPreview.id;
    this.sheetToPreview = null; // Ferme la modale

    this.previewService.reprocessSheet(sheetId, headerRowIndex).subscribe({
      next: () => {
        this.validatedSheets.add(sheetId);
        alert(`L'en-tête pour la feuille ${this.sheets.find(s => s.id === sheetId)?.sheetName} a été validé !`);
      },
      error: err => {
        alert("Une erreur est survenue lors du retraitement. Veuillez réessayer.");
      }
    });
  }

  allSheetsValidated(): boolean {
    return this.sheets.length === this.validatedSheets.size;
  }

  finishValidation() {
    alert("Validation terminée ! Vous allez être redirigé.");
    this.router.navigate(['/file', this.fileId]);
  }
}