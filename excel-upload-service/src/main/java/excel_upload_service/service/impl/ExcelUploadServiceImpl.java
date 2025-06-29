package excel_upload_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import excel_upload_service.config.rabbitmq.RabbitMQConfig;
import excel_upload_service.dto.UploadResponse;
import excel_upload_service.dto.audit.AuditRequest;
import excel_upload_service.model.FileEntity;
import excel_upload_service.repository.FileEntityRepository;
import excel_upload_service.service.AuditClientService;
import excel_upload_service.service.ExcelUploadService;
import excel_upload_service.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServiceImpl.class);

    private final FileStorageService fileStorageService;
    private final FileEntityRepository fileRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private final AuditClientService auditClientService;

    public ExcelUploadServiceImpl(FileStorageService fileStorageService, FileEntityRepository fileRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,  AuditClientService auditClientService) {
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.auditClientService = auditClientService;
    }

    @Override
    public UploadResponse processAndSave(MultipartFile file) {
        // 1. Sauvegarder le fichier sur le disque
        String storedFileName = fileStorageService.store(file);
        logger.info("Fichier {} stocké temporairement sous {}.", file.getOriginalFilename(), storedFileName);

        // 2. Créer une entrée dans la base de données pour ce fichier
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        // On pourrait ajouter un statut "EN_ATTENTE" ici
        fileRepository.save(fileEntity);

        // 3. Préparer le message à envoyer dans la file d'attente
        try {
            Map<String, Object> message = Map.of(
                    "fileId", fileEntity.getId(),
                    "originalFilename", file.getOriginalFilename(),
                    "storedFilename", storedFileName
            );
            String jsonMessage = objectMapper.writeValueAsString(message);

            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, jsonMessage);
            logger.info("Tâche pour le fichier {} envoyée à la file de traitement.", file.getOriginalFilename());

            // --- DÉBUT DE L'AJOUT DE L'AUDIT ---
            try {
                // Récupère l'utilisateur actuellement authentifié depuis le contexte de sécurité
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof UserDetails) {
                    String username = ((UserDetails) principal).getUsername();

                    // Construit et envoie la requête d'audit
                    AuditRequest auditRequest = AuditRequest.builder()
                            .username(username)
                            .action("FILE_UPLOAD")
                            .details("Fichier téléversé : " + file.getOriginalFilename())
                            .build();
                    auditClientService.logAction(auditRequest);
                }
            } catch (Exception e) {
                logger.error("Erreur non bloquante lors de la journalisation de l'audit pour l'upload.", e);
            }
            // --- FIN DE L'AJOUT DE L'AUDIT ---

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi du message à RabbitMQ", e);
            throw new RuntimeException("Impossible de mettre en file le traitement du fichier.");
        }

        // 5. Répondre immédiatement à l'utilisateur
        return new UploadResponse(true, "Le fichier a été reçu et est en cours de traitement.", null, 0);
    }
}