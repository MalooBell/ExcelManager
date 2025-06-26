package excel_upload_service.service;



import excel_upload_service.dto.UploadResponse;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface ExcelUploadService {
    UploadResponse uploadAndProcessExcel(MultipartFile file);
    /**
     * NOUVEAU : Retraite une seule feuille d'un fichier déjà uploadé.
     * @param sheetId L'ID de la feuille à retraiter.
     * @param headerRowIndex Le numéro de la ligne que l'utilisateur a désigné comme en-tête.
     * @throws IOException Si le fichier physique ne peut être lu.
     */
    void reprocessSheet(Long sheetId, int headerRowIndex) throws IOException;

}

