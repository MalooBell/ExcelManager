package excel_upload_service.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface FileStorageService {
    /**
     * Stocke un fichier et retourne son chemin unique.
     * @param file Le fichier à stocker.
     * @return Le chemin relatif unique du fichier stocké.
     */
    String store(MultipartFile file);
}