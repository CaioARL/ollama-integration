package com.caio.ollama_integration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.dto.AuthResponseDTO;
import com.caio.ollama_integration.exception.AuthenticationException;
import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.model.Role;
import com.caio.ollama_integration.model.User;
import com.caio.ollama_integration.repository.UserRepository;
import com.caio.ollama_integration.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Autenticação com usuário e senha
     */
    public AuthResponseDTO authenticate(String username, String password) {
        log.info("Tentativa de autenticação com username: {}", username);

        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado ou inativo: {}", username);
                    return new AuthenticationException("Credenciais inválidas");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Senha incorreta para usuário: {}", username);
            throw new AuthenticationException("Credenciais inválidas");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(username, roles);

        // Atualiza último login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Autenticação bem-sucedida para username: {} com roles: {}", username, roles);

        return AuthResponseDTO.builder()
                .token(token)
                .username(username)
                .roles(roles)
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * Cria novo usuário
     */
    public User createUser(String username, String password, Set<Role> roles, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidRequestException("Usuário já existe");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles(roles)
                .email(email)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    /**
     * Atualiza roles de um usuário
     */
    public User updateUserRoles(String username, Set<Role> newRoles) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidRequestException("Usuário não encontrado"));

        user.setRoles(newRoles);
        return userRepository.save(user);
    }

    /**
     * Desativa usuário
     */
    public void deactivateUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidRequestException("Usuário não encontrado"));

        user.setActive(false);
        userRepository.save(user);
        log.info("Usuário desativado: {}", username);
    }

    /**
     * Lista todos os usuários
     */
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Busca usuário por username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidRequestException("Usuário não encontrado"));
    }
}
