package com.caio.ollama_integration.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import com.caio.ollama_integration.hardcode.Role;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para atualizar roles de um usuário")
public class UpdateRolesRequestDTO {

    @NotEmpty(message = "Roles não podem ser vazias")
    @Schema(description = "Novas roles do usuário", example = "[\"USER\", \"ADMIN\"]")
    private Set<Role> roles;

}
