// CHEMIN : excel-upload-service/src/main/java/excel_upload_service/service/FileService.java
package excel_upload_service.service;

import excel_upload_service.dto.FileDto; // MODIFIÉ
import excel_upload_service.model.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FileService {
    // MODIFIÉ : La méthode retourne maintenant une Page de DTOs.
    Page<FileDto> getFiles(String searchKeyword, Pageable pageable);
    
    void deleteFile(Long id);
    
    // Cette méthode reste inchangée car pour un seul fichier, on veut tous les détails.
    FileEntity findById(Long id);
}