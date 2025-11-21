package com.caio.ollama_integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response contendo o token JWT e informações do usuário")
public class AuthResponse {

    @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Tipo do token", example = "Bearer")
    @Builder.Default
    private String type = "Bearer";

    @Schema(description = "Subject autenticado", example = "myapp")
    private String username;

    @Schema(description = "Tempo de expiração em milissegundos", example = "3600000")
    private Long expiresIn;
}
