package com.caio.ollama_integration.dto;

import com.caio.ollama_integration.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informações do usuário")
public class UserResponseDTO {

    @Schema(description = "ID do usuário", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "Nome de usuário", example = "johndoe")
    private String username;

    @Schema(description = "Email do usuário", example = "john@example.com")
    private String email;

    @Schema(description = "Roles do usuário")
    private Set<Role> roles;

    @Schema(description = "Se o usuário está ativo", example = "true")
    private Boolean active;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Schema(description = "Último login")
    private LocalDateTime lastLogin;

}
