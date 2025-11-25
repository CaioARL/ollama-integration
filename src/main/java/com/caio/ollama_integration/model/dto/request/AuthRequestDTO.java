package com.caio.ollama_integration.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para autenticação do usuário")
public class AuthRequestDTO {

    @NotBlank(message = "Username é obrigatório")
    @Schema(description = "Nome de usuário", example = "admin")
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", example = "admin123")
    private String password;
}
