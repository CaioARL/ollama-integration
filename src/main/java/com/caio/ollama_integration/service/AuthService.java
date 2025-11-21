package com.caio.ollama_integration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.dto.AuthRequest;
import com.caio.ollama_integration.dto.AuthResponse;
import com.caio.ollama_integration.exception.AuthenticationException;
import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${auth.subject}")
    private String configuredSubject;

    @Value("${auth.access-key}")
    private String configuredAccessKey;

    public AuthResponse authenticate(AuthRequest request) {
        log.info("Tentativa de autenticação com subject: {}", request.getSubject());

        validateCredentials(request);

        String token = jwtUtil.generateToken(request.getSubject());

        log.info("Autenticação bem-sucedida para subject: {}", request.getSubject());

        return AuthResponse.builder()
                .token(token)
                .username(request.getSubject())
                .expiresIn(jwtExpiration)
                .build();
    }

    private void validateCredentials(AuthRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request de autenticação não pode ser nulo");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            log.warn("Subject não fornecido");
            throw new InvalidRequestException("Subject é obrigatório");
        }
        if (request.getAccessKey() == null || request.getAccessKey().trim().isEmpty()) {
            log.warn("AccessKey não fornecido");
            throw new InvalidRequestException("AccessKey é obrigatório");
        }

        if (!configuredSubject.equals(request.getSubject()) ||
                !configuredAccessKey.equals(request.getAccessKey())) {
            log.warn("Falha na autenticação - credenciais inválidas para subject: {}", request.getSubject());
            throw new AuthenticationException("Credenciais inválidas");
        }
    }
}
