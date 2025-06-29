package excel_upload_service.service;

import excel_upload_service.dto.audit.AuditRequest;

public interface AuditClientService {
    /**
     * Envoie une requête de journalisation au service d'audit.
     * @param request Les détails de l'action à journaliser.
     */
    void logAction(AuditRequest request);
}