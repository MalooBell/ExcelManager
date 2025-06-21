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

    // This endpoint now gets rows for a specific file, with search and sorting.
    @GetMapping("/file/{fileId}")
    public Page<RowEntityDto> getRowsForFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return service.searchByFileId(fileId, keyword, pageable);
    }

    // The generic search is still useful for a global search view.
    @GetMapping
    public Page<RowEntityDto> searchAll(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return service.search(fileName, keyword, pageable);
    }

    @GetMapping("/{id}")
    public RowEntityDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // When creating a row, we must associate it with a file.
    @PostMapping("/file/{fileId}")
    public RowEntityDto create(@PathVariable Long fileId, @RequestBody RowEntityDto dto) {
        return service.create(fileId, dto);
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