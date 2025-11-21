package com.caio.ollama_integration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caio.ollama_integration.dto.ChatRequest;
import com.caio.ollama_integration.dto.ChatResponse;
import com.caio.ollama_integration.service.OllamaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints para interação com o agente de IA Ollama")
@SecurityRequirement(name = "Bearer Authentication")
public class ChatController {

    private final OllamaService ollamaService;

    @PostMapping
    @Operation(summary = "Enviar mensagem para o agente de IA", description = "Envia uma mensagem para o modelo Ollama e recebe uma resposta. Requer autenticação JWT.")
    public ResponseEntity<ChatResponse> chat(@RequestBody(required = true) ChatRequest request) {
        ChatResponse response = ollamaService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models")
    @Operation(summary = "Listar modelos disponíveis no Ollama", description = "Retorna a lista de modelos de linguagem disponíveis no Ollama. Requer autenticação JWT.")
    public ResponseEntity<String> listModels() {
        String models = ollamaService.listModels();
        return ResponseEntity.ok(models);
    }
}
