package com.caio.ollama_integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informações detalhadas de um modelo Ollama")
public class ModelResponseDTO {

    @Schema(description = "Nome do modelo", example = "llama3.2:latest")
    private String name;

    @Schema(description = "Data de modificação do modelo")
    private Instant modifiedAt;

    @Schema(description = "Tamanho do modelo em bytes", example = "2000000000")
    private Long size;

    @Schema(description = "Tamanho do modelo formatado", example = "2.00 GB")
    private String formattedSize;

    @Schema(description = "Digest SHA256 do modelo", example = "sha256:abc123...")
    private String digest;

    @Schema(description = "Detalhes adicionais do modelo")
    private ModelDetailsDTO details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detalhes técnicos do modelo")
    public static class ModelDetailsDTO {

        @Schema(description = "Formato do modelo", example = "gguf")
        private String format;

        @Schema(description = "Família do modelo", example = "llama")
        private String family;

        @Schema(description = "Famílias suportadas pelo modelo")
        private List<String> families;

        @Schema(description = "Tamanho dos parâmetros", example = "3.2B")
        private String parameterSize;

        @Schema(description = "Tamanho de quantização", example = "Q4_0")
        private String quantizationLevel;

        @Schema(description = "Metadados adicionais do modelo")
        private Map<String, Object> metadata;
    }
}
