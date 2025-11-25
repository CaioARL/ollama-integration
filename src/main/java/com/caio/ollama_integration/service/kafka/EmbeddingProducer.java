package com.caio.ollama_integration.service.kafka;

import com.caio.ollama_integration.model.kafka.EmbeddingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Producer Kafka para enfileirar requisições de embedding
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingProducer {

    private static final String TOPIC = "embedding-requests";

    private final KafkaTemplate<String, EmbeddingRequestEvent> kafkaTemplate;

    /**
     * Envia requisição de embedding para fila Kafka
     */
    public CompletableFuture<String> sendEmbeddingRequest(
            String content,
            Map<String, Object> metadata,
            String username,
            String documentType) {

        String eventId = UUID.randomUUID().toString();

        EmbeddingRequestEvent event = EmbeddingRequestEvent.builder()
                .eventId(eventId)
                .content(content)
                .metadata(metadata)
                .username(username)
                .documentType(documentType)
                .timestamp(System.currentTimeMillis())
                .retryCount(0)
                .build();

        log.info("[KAFKA PRODUCER] Enviando evento {} para tópico '{}' - user: {}, type: {}",
                eventId, TOPIC, username, documentType);

        CompletableFuture<SendResult<String, EmbeddingRequestEvent>> future = kafkaTemplate.send(TOPIC, username,
                event);

        return future.handle((result, ex) -> {
            if (ex != null) {
                log.error("[KAFKA PRODUCER] Erro ao enviar evento {}: {}", eventId, ex.getMessage(), ex);
                throw new RuntimeException("Falha ao enfileirar embedding", ex);
            } else {
                log.info("[KAFKA PRODUCER] Evento {} enviado com sucesso - partition: {}, offset: {}",
                        eventId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return eventId;
            }
        });
    }

    /**
     * Envia múltiplas requisições em lote
     */
    public CompletableFuture<Void> sendBatchEmbeddingRequests(
            java.util.List<EmbeddingRequestEvent> events) {

        log.info("[KAFKA PRODUCER] Enviando batch de {} eventos", events.size());

        CompletableFuture<?>[] futures = events.stream()
                .map(event -> kafkaTemplate.send(TOPIC, event.getUsername(), event))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenRun(() -> log.info("[KAFKA PRODUCER] Batch de {} eventos enviado", events.size()));
    }
}
