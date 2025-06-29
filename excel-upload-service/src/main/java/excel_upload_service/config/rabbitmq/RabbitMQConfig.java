// CHEMIN: excel-upload-service/src/main/java/excel_upload_service/config/rabbitmq/RabbitMQConfig.java
package excel_upload_service.config.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nom de notre file d'attente. Il sera utilisé par Java et Python.
    public static final String QUEUE_NAME = "excel-processing-queue";

    @Bean
    public Queue excelProcessingQueue() {
        // Déclare une file d'attente durable.
        // "durable" signifie que la file survivra à un redémarrage de RabbitMQ.
        return new Queue(QUEUE_NAME, true);
    }
}