package com.caio.ollama_integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    @Schema(description = "Mensagem a ser enviada ao agente de IA", example = "Olá, como você está?")
    @NotBlank(message = "Mensagem não pode estar em branco")
    private String message;

    @Schema(description = "Modelo do agente de IA a ser utilizado", example = "llama3.1")
    @NotBlank(message = "Modelo não pode estar em branco")
    private String model;

    @Schema(description = "Temperatura para geração de texto (controle de aleatoriedade)", example = "0.7")
    @PositiveOrZero(message = "Temperatura deve ser zero ou positiva")
    private Double temperature;
}
