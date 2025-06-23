// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/controller/RowEntityController.java
package excel_upload_service.controller;

import excel_upload_service.dto.RowEntityDto;
import excel_upload_service.service.ExcelDownloadService;
import excel_upload_service.service.RowEntityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/rows")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RowEntityController {

    private final RowEntityService service;
    private final ExcelDownloadService downloadService;

    public RowEntityController(RowEntityService service, ExcelDownloadService downloadService) {
        this.service = service;
        this.downloadService = downloadService;
    }

    // L'endpoint principal pour lister les lignes est maintenant basé sur sheetId
    @GetMapping("/sheet/{sheetId}")
    public Page<RowEntityDto> getRowsForSheet(
            @PathVariable Long sheetId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return service.searchBySheetId(sheetId, keyword, pageable);
    }

    @GetMapping("/{id}")
    public RowEntityDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // La création d'une ligne se fait maintenant dans le contexte d'une feuille
    @PostMapping("/sheet/{sheetId}")
    public RowEntityDto create(@PathVariable Long sheetId, @RequestBody RowEntityDto dto) {
        return service.create(sheetId, dto);
    }

    @PutMapping("/{id}")
    public RowEntityDto update(@PathVariable Long id, @RequestBody RowEntityDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // L'endpoint de téléchargement est aussi basé sur sheetId
    @GetMapping("/sheet/{sheetId}/download")
    public void downloadExcel(
            @PathVariable Long sheetId,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {
        downloadService.downloadSheetData(sheetId, keyword, response);
    }

    
}