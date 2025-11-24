package com.caio.ollama_integration.security;

import com.caio.ollama_integration.model.Role;
import com.caio.ollama_integration.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleCheckInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresRole requiresRole = handlerMethod.getMethodAnnotation(RequiresRole.class);

        if (requiresRole == null) {
            return true; // Sem anotação, permite acesso
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        List<String> userRoles = jwtUtil.extractRoles(token);

        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("Usuário sem roles tentando acessar endpoint protegido");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        Role[] requiredRoles = requiresRole.value();
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> userRoles.contains(role.name()));

        if (!hasRequiredRole) {
            log.warn("Usuário sem permissão necessária. Roles do usuário: {}, Roles necessárias: {}",
                    userRoles, Arrays.toString(requiredRoles));
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
