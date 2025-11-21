package com.caio.ollama_integration.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response de erro padronizado")
public class ErrorResponse {

    @Schema(description = "Código de status HTTP", example = "400")
    private int status;

    @Schema(description = "Mensagem de erro principal", example = "Requisição inválida")
    private String error;

    @Schema(description = "Mensagem detalhada do erro", example = "Mensagem não pode ser vazia")
    private String message;

    @Schema(description = "Path da requisição que gerou o erro", example = "/v1/api/chat")
    private String path;

    @Schema(description = "Timestamp do erro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
