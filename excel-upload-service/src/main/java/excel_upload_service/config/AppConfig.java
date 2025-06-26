// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/config/AppConfig.java
package excel_upload_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Définit un bean RestTemplate pour effectuer des appels HTTP synchrones.
     * Ce bean pourra être injecté dans n'importe quel autre composant Spring.
     * @return Une nouvelle instance de RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}