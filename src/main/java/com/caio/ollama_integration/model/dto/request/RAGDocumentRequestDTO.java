package com.caio.ollama_integration.model.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para adicionar documento RAG")
public class RAGDocumentRequestDTO {

    @NotBlank(message = "O conteúdo é obrigatório")
    @Size(min = 10, max = 10000, message = "O conteúdo deve ter entre 10 e 10000 caracteres")
    @Schema(description = "Conteúdo do documento", example = "Spring Boot é um framework Java...")
    private String content;

    @Schema(description = "Título do documento", example = "Tutorial Spring Boot")
    private String title;

    @Schema(description = "Fonte do documento", example = "docs.spring.io")
    private String source;

    @Schema(description = "Tipo do documento", example = "knowledge", defaultValue = "knowledge")
    @Builder.Default
    private String documentType = "knowledge";

    @Schema(description = "Tags para categorização", example = "[\"java\", \"spring\", \"backend\"]")
    private List<String> tags;
}
