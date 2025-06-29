package excel_upload_service.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ce DTO est une copie de celui du service utilisateur.
// Il sert à construire la requête JSON à envoyer.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditRequest {
    private String username;
    private String action;
    private String details;
}