package com.caio.ollama_integration.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Evento de resultado de processamento de embedding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResultEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String documentId;
    private String username;
    private String documentType;
    private boolean success;
    private String errorMessage;
    private Long timestamp;
    private Long processingTimeMs;
}
