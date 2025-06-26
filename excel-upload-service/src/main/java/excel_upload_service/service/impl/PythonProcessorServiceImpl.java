// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/service/impl/PythonProcessorServiceImpl.java
package excel_upload_service.service.impl;

import excel_upload_service.dto.python.ExcelProcessingResponse;
import excel_upload_service.service.PythonProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PythonProcessorServiceImpl implements PythonProcessorService {

    private final RestTemplate restTemplate;

    // L'URL du service Python est configurable dans application.yml
    @Value("${python.processor.url}")
    private String pythonProcessorUrl;

    public PythonProcessorServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ExcelProcessingResponse processFile(MultipartFile file) {
        // 1. Préparer les en-têtes de la requête
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 2. Préparer le corps de la requête (le fichier lui-même)
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // On a besoin de convertir MultipartFile en une ressource que RestTemplate peut gérer.
        Resource fileAsResource = file.getResource();
        body.add("file", fileAsResource);

        // 3. Créer l'entité de la requête HTTP (en-têtes + corps)
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 4. Construire l'URL complète de l'API Python
        String serverUrl = pythonProcessorUrl + "/process-excel";

        // 5. Envoyer la requête POST et récupérer la réponse
        // RestTemplate s'occupe de la désérialisation du JSON dans notre objet DTO.
        try {
            return restTemplate.postForObject(serverUrl, requestEntity, ExcelProcessingResponse.class);
        } catch (Exception e) {
            // En cas d'échec de la communication, on lève une exception pour que l'appelant puisse la gérer.
            throw new RuntimeException("Erreur lors de la communication avec le service de traitement Python: " + e.getMessage(), e);
        }
    }
}