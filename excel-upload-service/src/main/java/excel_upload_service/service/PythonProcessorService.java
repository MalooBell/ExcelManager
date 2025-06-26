// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/PythonProcessorService.java
package excel_upload_service.service;

import excel_upload_service.dto.python.ExcelProcessingResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PythonProcessorService {
    /**
     * Envoie un fichier au microservice Python pour traitement.
     * @param file Le fichier Excel à traiter.
     * @return Un objet contenant les données structurées du fichier.
     */
    ExcelProcessingResponse processFile(MultipartFile file);
}