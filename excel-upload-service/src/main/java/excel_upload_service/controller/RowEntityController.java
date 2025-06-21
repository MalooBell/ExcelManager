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
@CrossOrigin
public class RowEntityController {

    private final RowEntityService service;
    private final ExcelDownloadService downloadService; // NOUVEAU SERVICE

    public RowEntityController(RowEntityService service, ExcelDownloadService downloadService) {
        this.service = service;
        this.downloadService = downloadService;
    }

    // MÉTHODE "getAll" MODIFIÉE POUR GÉRER LA RECHERCHE
    @GetMapping
    public Page<RowEntityDto> getAll(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        // Si aucun paramètre de recherche n'est fourni, on retourne tout
        if ((fileName == null || fileName.isEmpty()) && (keyword == null || keyword.isEmpty())) {
            return service.getAll(pageable);
        }
        // Sinon, on effectue une recherche
        return service.search(fileName, keyword, pageable);
    }

    @GetMapping("/{id}")
    public RowEntityDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public RowEntityDto create(@RequestBody RowEntityDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public RowEntityDto update(@PathVariable Long id, @RequestBody RowEntityDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/download")
    public void downloadExcel(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {
        downloadService.downloadFilteredData(fileName, keyword, response);
    }
}