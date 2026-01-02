package com.caio.ollama_integration.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response de documento RAG")
public class RAGDocumentResponseDTO {

    @Schema(description = "ID do documento", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "Conteúdo do documento")
    private String content;

    @Schema(description = "Tipo do documento", example = "knowledge")
    private String documentType;

    @Schema(description = "Metadados do documento")
    private Map<String, Object> metadata;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
}
