package com.caio.ollama_integration.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caio.ollama_integration.hardcode.Role;
import com.caio.ollama_integration.model.User;
import com.caio.ollama_integration.model.dto.request.CreateUserRequestDTO;
import com.caio.ollama_integration.model.dto.response.UpdateRolesRequestDTO;
import com.caio.ollama_integration.model.dto.response.UserResponseDTO;
import com.caio.ollama_integration.security.RequiresRole;
import com.caio.ollama_integration.service.AuthService;
import com.caio.ollama_integration.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("v1/admin")
@Tag(name = "Administração", description = "Endpoints de administração (apenas ADMIN)")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final ConversationService conversationService;

    @PostMapping("/users")
    @RequiresRole(Role.ADMIN)
    @Operation(summary = "Criar novo usuário", description = "Cria um novo usuário no sistema. Apenas administradores podem usar este endpoint.")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        log.info("Admin criando novo usuário: {}", request.getUsername());

        Set<Role> roles = request.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add(Role.USER);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponseDTO(authService.createUser(
                request.getUsername(),
                request.getPassword(),
                roles,
                request.getEmail())));
    }

    @GetMapping("/users")
    @RequiresRole({ Role.ADMIN, Role.MODERATOR })
    @Operation(summary = "Listar todos os usuários", description = "Lista todos os usuários do sistema. Acessível por ADMIN e MODERATOR.")
    public ResponseEntity<List<UserResponseDTO>> listAllUsers() {
        log.info("Listando todos os usuários");

        return ResponseEntity.ok(authService.listAllUsers().stream()
                .map(this::toUserResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/users/{username}")
    @RequiresRole({ Role.ADMIN, Role.MODERATOR })
    @Operation(summary = "Obter usuário específico", description = "Retorna detalhes de um usuário específico. Acessível por ADMIN e MODERATOR.")
    public ResponseEntity<UserResponseDTO> getUser(
            @Parameter(description = "Nome de usuário") @PathVariable String username) {
        log.info("Buscando usuário: {}", username);

        return ResponseEntity.ok(toUserResponseDTO(authService.getUserByUsername(username)));
    }

    @PatchMapping("/users/{username}/roles")
    @RequiresRole(Role.ADMIN)
    @Operation(summary = "Atualizar roles de usuário", description = "Atualiza as permissões (roles) de um usuário. Apenas ADMIN pode usar este endpoint.")
    public ResponseEntity<UserResponseDTO> updateUserRoles(
            @Parameter(description = "Nome de usuário") @PathVariable String username,
            @Valid @RequestBody UpdateRolesRequestDTO request) {
        log.info("Atualizando roles do usuário: {} para: {}", username, request.getRoles());

        return ResponseEntity.ok(toUserResponseDTO(authService.updateUserRoles(username, request.getRoles())));
    }

    @DeleteMapping("/users/{username}")
    @RequiresRole(Role.ADMIN)
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário do sistema. Apenas ADMIN pode usar este endpoint.")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "Nome de usuário") @PathVariable String username) {
        log.info("Desativando usuário: {}", username);

        authService.deactivateUser(username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/user/{username}")
    @RequiresRole({ Role.ADMIN, Role.MODERATOR })
    @Operation(summary = "Listar conversações de um usuário específico", description = "Lista todas as conversações de um usuário. Acessível por ADMIN e MODERATOR.")
    public ResponseEntity<?> listUserConversations(
            @Parameter(description = "Nome de usuário") @PathVariable String username) {
        log.info("Listando conversações do usuário: {}", username);

        var conversations = conversationService.listConversations(username, null);

        return ResponseEntity.ok(conversations);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @RequiresRole(Role.ADMIN)
    @Operation(summary = "Deletar qualquer conversação", description = "Deleta uma conversação de qualquer usuário. Apenas ADMIN pode usar este endpoint.")
    public ResponseEntity<Void> deleteAnyConversation(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @AuthenticationPrincipal String username) {
        log.info("Admin deletando conversação: {}", conversationId);

        conversationService.deleteConversation(conversationId, username);
        return ResponseEntity.noContent().build();
    }

    // Métodos auxiliares

    private UserResponseDTO toUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

}
