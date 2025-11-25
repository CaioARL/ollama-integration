package com.caio.ollama_integration.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com informações sobre o status da fila de embeddings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponseDTO {

    private String queueName;
    private int activeThreads;
    private int poolSize;
    private int corePoolSize;
    private int maxPoolSize;
    private int queueSize;
    private int queueCapacity;
    private long completedTasks;
    private boolean isHealthy;
    private String status;
}
