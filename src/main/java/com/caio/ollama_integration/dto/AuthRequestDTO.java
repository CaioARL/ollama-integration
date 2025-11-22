package com.caio.ollama_integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para autenticação do usuário")
public class AuthRequestDTO {

    @Schema(description = "Subject do aplicativo", example = "myapp")
    private String subject;

    @Schema(description = "Chave de acesso (Access Key)", example = "secretkey123")
    private String accessKey;
}
