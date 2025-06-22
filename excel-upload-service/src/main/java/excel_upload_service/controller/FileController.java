package excel_upload_service.controller;

import excel_upload_service.model.FileEntity;
import excel_upload_service.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String search, // AJOUT DE CE PARAMÈTRE
            Pageable pageable) {
        return fileService.getFiles(search, pageable); // MODIFICATION
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
}