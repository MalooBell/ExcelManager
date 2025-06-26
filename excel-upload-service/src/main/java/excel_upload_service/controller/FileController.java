// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/FileController.java
package excel_upload_service.controller;

import excel_upload_service.model.FileEntity;
import excel_upload_service.model.SheetEntity; // NOUVEAU
import excel_upload_service.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public Page<FileEntity> getFiles(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return fileService.getFiles(search, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileEntity> getFileById(@PathVariable Long id) {
        FileEntity file = fileService.findById(id);
        return ResponseEntity.ok(file);
    }

    // NOUVEL ENDPOINT pour lister les feuilles d'un fichier
    @GetMapping("/{id}/sheets")
    public ResponseEntity<List<SheetEntity>> getSheetsForFile(@PathVariable Long id) {
        FileEntity file = fileService.findById(id);
        // On trie par l'index de la feuille pour un affichage cohérent
        List<SheetEntity> sortedSheets = file.getSheets().stream()
                .sorted((s1, s2) -> Integer.compare(s1.getSheetIndex(), s2.getSheetIndex()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sortedSheets);
    }
}