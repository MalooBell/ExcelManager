package excel_upload_service.service;

import excel_upload_service.dto.RowEntityDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RowEntityService {
    Page<RowEntityDto> getAll(Pageable pageable);
    RowEntityDto getById(Long id);
    RowEntityDto create(Long fileId, RowEntityDto dto); // fileId is now required
    RowEntityDto update(Long id, RowEntityDto dto);
    void delete(Long id);

    Page<RowEntityDto> search(String fileName, String keyword, Pageable pageable);
    Page<RowEntityDto> searchByFileId(Long fileId, String keyword, Pageable pageable);
}