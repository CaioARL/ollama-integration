package com.caio.ollama_integration.service.kafka;

import com.caio.ollama_integration.model.kafka.EmbeddingRequestEvent;
import com.caio.ollama_integration.model.kafka.EmbeddingResultEvent;
import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;
import com.caio.ollama_integration.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Consumer Kafka para processar requisições de embedding
 * Escalável horizontalmente - pode rodar múltiplas instâncias
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingConsumer {

    private static final String REQUEST_TOPIC = "embedding-requests";
    private static final String RESULT_TOPIC = "embedding-results";
    private static final String DLQ_TOPIC = "embedding-dlq"; // Dead Letter Queue

    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, EmbeddingResultEvent> resultKafkaTemplate;

    /**
     * Processa requisições de embedding da fila Kafka
     * Múltiplas instâncias podem processar em paralelo
     */
    @KafkaListener(topics = REQUEST_TOPIC, groupId = "${spring.kafka.consumer.group-id:embedding-service-group}", containerFactory = "embeddingRequestKafkaListenerContainerFactory")
    public void processEmbeddingRequest(
            @Payload EmbeddingRequestEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        long startTime = System.currentTimeMillis();

        log.info("[KAFKA CONSUMER] Processando evento {} da partition {} offset {} - user: {}, type: {}",
                event.getEventId(), partition, offset, event.getUsername(), event.getDocumentType());

        try {
            // Processa embedding de forma síncrona dentro do consumer
            EmbeddingDocument document = embeddingService.createDocument(
                    event.getContent(),
                    event.getMetadata(),
                    event.getUsername(),
                    event.getDocumentType());

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("[KAFKA CONSUMER] Embedding processado com sucesso - evento: {}, docId: {}, tempo: {}ms",
                    event.getEventId(), document.getId(), processingTime);

            // Publica resultado de sucesso
            publishResult(event, document.getId(), true, null, processingTime);

            // Confirma processamento (commit manual)
            acknowledgment.acknowledge();

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            log.error("[KAFKA CONSUMER] Erro ao processar evento {} após {}ms: {}",
                    event.getEventId(), processingTime, e.getMessage(), e);

            // Publica resultado de erro
            publishResult(event, null, false, e.getMessage(), processingTime);

            // Verifica se deve retentar ou enviar para DLQ
            if (event.getRetryCount() != null && event.getRetryCount() >= 3) {
                log.error("[KAFKA CONSUMER] Evento {} excedeu tentativas, enviando para DLQ", event.getEventId());
                sendToDLQ(event, e);
                acknowledgment.acknowledge(); // Remove da fila principal
            } else {
                // Não confirma para que seja reprocessado
                log.warn("[KAFKA CONSUMER] Evento {} será reprocessado (tentativa {}/3)",
                        event.getEventId(), (event.getRetryCount() != null ? event.getRetryCount() + 1 : 1));
                // Incrementa contador de retry para próxima tentativa
                event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
            }
        }
    }

    /**
     * Publica resultado do processamento
     */
    private void publishResult(EmbeddingRequestEvent request, String documentId,
            boolean success, String errorMessage, long processingTime) {
        EmbeddingResultEvent result = EmbeddingResultEvent.builder()
                .eventId(request.getEventId())
                .documentId(documentId)
                .username(request.getUsername())
                .documentType(request.getDocumentType())
                .success(success)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .processingTimeMs(processingTime)
                .build();

        resultKafkaTemplate.send(RESULT_TOPIC, request.getUsername(), result)
                .whenComplete((sendResult, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA CONSUMER] Erro ao publicar resultado: {}", ex.getMessage());
                    } else {
                        log.debug("[KAFKA CONSUMER] Resultado publicado: {}", result.getEventId());
                    }
                });
    }

    /**
     * Envia evento para Dead Letter Queue
     */
    private void sendToDLQ(EmbeddingRequestEvent event, Exception error) {
        try {
            // Adiciona informação do erro aos metadados
            if (event.getMetadata() == null) {
                event.setMetadata(new java.util.HashMap<>());
            }
            event.getMetadata().put("dlq_reason", error.getMessage());
            event.getMetadata().put("dlq_timestamp", System.currentTimeMillis());

            resultKafkaTemplate.send(DLQ_TOPIC, event.getUsername(),
                    EmbeddingResultEvent.builder()
                            .eventId(event.getEventId())
                            .username(event.getUsername())
                            .documentType(event.getDocumentType())
                            .success(false)
                            .errorMessage("Moved to DLQ: " + error.getMessage())
                            .timestamp(System.currentTimeMillis())
                            .build());

            log.info("[KAFKA CONSUMER] Evento {} enviado para DLQ", event.getEventId());
        } catch (Exception e) {
            log.error("[KAFKA CONSUMER] Erro ao enviar para DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Listener opcional para monitorar resultados
     */
    @KafkaListener(topics = RESULT_TOPIC, groupId = "${spring.kafka.consumer.group-id:embedding-service-group}-result", containerFactory = "embeddingResultKafkaListenerContainerFactory")
    public void monitorResults(@Payload EmbeddingResultEvent result) {
        if (result.isSuccess()) {
            log.info("[KAFKA MONITOR] ✓ Embedding concluído - evento: {}, docId: {}, tempo: {}ms",
                    result.getEventId(), result.getDocumentId(), result.getProcessingTimeMs());
        } else {
            log.warn("[KAFKA MONITOR] ✗ Embedding falhou - evento: {}, erro: {}",
                    result.getEventId(), result.getErrorMessage());
        }
    }
}
