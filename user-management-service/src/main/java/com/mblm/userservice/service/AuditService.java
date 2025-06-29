package com.mblm.userservice.service;

import com.mblm.userservice.dto.AuditRequest;
import com.mblm.userservice.model.AuditLog;
import com.mblm.userservice.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(AuditRequest request) {
        if (request.getUsername() == null || request.getAction() == null) {
            logger.warn("Tentative de journalisation d'une action invalide : {}", request);
            return;
        }

        AuditLog logEntry = AuditLog.builder()
                .username(request.getUsername())
                .action(request.getAction())
                .details(request.getDetails())
                .build();

        auditLogRepository.save(logEntry);
        logger.info("Action journalisée : [Utilisateur: {}, Action: {}]", request.getUsername(), request.getAction());
    }
}