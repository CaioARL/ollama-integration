package com.caio.ollama_integration.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para criar ou atualizar conversação")
public class ConversationRequestDTO {

    @NotBlank(message = "A mensagem é obrigatória")
    @Schema(description = "Mensagem do usuário", example = "O que é inteligência artificial?")
    private String message;

    @Builder.Default
    @Schema(description = "Modelo LLM a ser usado", example = "llama3.2", defaultValue = "llama3.2")
    private String model = "llama3.2";

    @Builder.Default
    @Schema(description = "Temperatura para controle de criatividade", example = "0.7", defaultValue = "0.7")
    private Double temperature = 0.7;

}
