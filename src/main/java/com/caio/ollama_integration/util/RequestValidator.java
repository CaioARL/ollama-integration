package com.caio.ollama_integration.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestValidator {

    public void validateChatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.warn("Mensagem de chat vazia ou inválida recebida");
            throw new IllegalArgumentException("Mensagem não pode ser vazia");
        }
    }
}
