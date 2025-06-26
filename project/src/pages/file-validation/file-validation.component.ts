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
        this.loading = false;
        
        // --- NEW LOGIC FOR AUTOMATIC MODAL OPENING ---
        // If the file requires header validation and not all sheets are yet validated,
        // open the modal for the first unvalidated sheet.
        if (this.file?.needsHeaderValidation && !this.allSheetsValidated()) {
          const firstUnvalidatedSheet = this.sheets.find(sheet => !this.validatedSheets.has(sheet.id));
          if (firstUnvalidatedSheet) {
            this.openPreview(firstUnvalidatedSheet);
          }
        }
        // --- END NEW LOGIC ---
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

    // Important: Re-fetch the file and sheets to ensure the latest validation status is reflected
    // and to potentially update the 'needsHeaderValidation' flag from the backend after a sheet is processed.
    this.previewService.reprocessSheet(sheetId, headerRowIndex).subscribe({
      next: () => {
        this.validatedSheets.add(sheetId);
        // Display a more subtle message or integrate into UI, avoiding `alert()`
        console.log(`L'en-tête pour la feuille ${this.sheets.find(s => s.id === sheetId)?.sheetName} a été validé !`);
        // Check if there are more sheets to validate and open the next modal if so
        this.checkForNextUnvalidatedSheet();
      },
      error: err => {
        // Display a more subtle message or integrate into UI, avoiding `alert()`
        console.error("Une erreur est survenue lors du retraitement. Veuillez réessayer.", err);
      }
    });
  }

  allSheetsValidated(): boolean {
    // Check if the total number of sheets matches the number of validated sheets
    // And that there's at least one sheet in the file.
    return this.sheets.length > 0 && this.sheets.length === this.validatedSheets.size;
  }

  // --- NEW METHOD FOR SEQUENTIAL MODAL OPENING ---
  checkForNextUnvalidatedSheet(): void {
    if (this.allSheetsValidated()) {
      // If all sheets are validated, you might want to automatically finish validation
      // or simply leave the "Terminer et Traiter le Fichier" button enabled.
      // For now, we'll let the user click the button.
    } else {
      const nextUnvalidatedSheet = this.sheets.find(sheet => !this.validatedSheets.has(sheet.id));
      if (nextUnvalidatedSheet) {
        this.openPreview(nextUnvalidatedSheet);
      }
    }
  }
  // --- END NEW METHOD ---

  finishValidation() {
    // IMPORTANT: Avoid using alert() in production applications, especially in IFrames.
    // Use a custom modal or toast notification system for better user experience.
    console.log("Validation terminée ! Vous allez être redirigé."); // Log instead of alert
    this.router.navigate(['/file', this.fileId]);
  }
}