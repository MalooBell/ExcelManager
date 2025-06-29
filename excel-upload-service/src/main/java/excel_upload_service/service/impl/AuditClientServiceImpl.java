package excel_upload_service.service.impl;

import excel_upload_service.dto.audit.AuditRequest;
import excel_upload_service.service.AuditClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuditClientServiceImpl implements AuditClientService {

    private static final Logger logger = LoggerFactory.getLogger(AuditClientServiceImpl.class);

    private final RestTemplate restTemplate;

    // L'URL du service de gestion des utilisateurs, à configurer
    @Value("${user.management.service.url}")
    private String userServiceUrl;

    public AuditClientServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void logAction(AuditRequest request) {
        String auditUrl = userServiceUrl + "/api/audit/log";

        try {
            // Note : Dans une architecture de microservices mature, le token de l'utilisateur
            // serait propagé, ou le service utiliserait son propre token de service-à-service.
            // Pour l'instant, nous faisons confiance au réseau interne.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AuditRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(auditUrl, entity, String.class);
            logger.info("Événement d'audit envoyé avec succès pour l'utilisateur '{}'", request.getUsername());
        } catch (RestClientException e) {
            // Si le service d'audit est indisponible, on logue l'erreur mais on ne bloque pas
            // l'action principale. L'audit est important, mais pas au point de paralyser l'application.
            logger.error("Impossible d'envoyer l'événement d'audit au service utilisateur. Erreur: {}", e.getMessage());
        }
    }
}