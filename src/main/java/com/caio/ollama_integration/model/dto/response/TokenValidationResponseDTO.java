package com.caio.ollama_integration.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response de validação de token JWT")
public class TokenValidationResponseDTO {

    @Schema(description = "Indica se o token é válido", example = "true")
    private Boolean valid;

    @Schema(description = "Nome de usuário extraído do token", example = "admin")
    private String username;

    @Schema(description = "Mensagem de erro se o token for inválido", example = "Token expirado")
    private String message;
}
