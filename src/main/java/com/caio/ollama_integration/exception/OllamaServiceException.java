package com.caio.ollama_integration.exception;

public class OllamaServiceException extends RuntimeException {
    public OllamaServiceException(String message) {
        super(message);
    }

    public OllamaServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
