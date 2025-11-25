package com.caio.ollama_integration.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Evento para requisição de criação de embedding via Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequestEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String content;
    private Map<String, Object> metadata;
    private String username;
    private String documentType;
    private Long timestamp;
    private Integer retryCount;
}
