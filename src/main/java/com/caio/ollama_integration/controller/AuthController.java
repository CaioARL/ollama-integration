package com.caio.ollama_integration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caio.ollama_integration.dto.AuthRequestDTO;
import com.caio.ollama_integration.dto.AuthResponseDTO;
import com.caio.ollama_integration.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("v1/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação e gerenciamento de tokens JWT")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Fazer login e gerar token JWT", description = "Autentica usando username e password. Retorna um token JWT válido para uso no header Authorization como 'Bearer {token}'")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        log.info("Tentativa de login: {}", request.getUsername());
        AuthResponseDTO response = authService.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Validar token JWT", description = "Verifica se o token é válido. Requer autenticação.")
    public ResponseEntity<Void> validateToken() {
        return ResponseEntity.noContent().build();
    }
}
