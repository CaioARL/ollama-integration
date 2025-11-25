package com.caio.ollama_integration.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lista de modelos disponíveis no Ollama")
public class ModelsListResponseDTO {

    @Schema(description = "Total de modelos disponíveis", example = "4")
    private Integer totalModels;

    @Schema(description = "Tamanho total dos modelos em bytes", example = "8000000000")
    private Long totalSize;

    @Schema(description = "Tamanho total formatado", example = "8.00 GB")
    private String formattedTotalSize;

    @Schema(description = "Lista de modelos disponíveis")
    private List<ModelResponseDTO> models;
}
