package excel_upload_service.service;



import excel_upload_service.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelUploadService {
    UploadResponse uploadAndProcessExcel(MultipartFile file);

}

