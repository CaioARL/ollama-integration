package com.caio.ollama_integration.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaApi.ListModelResponse;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.dto.ChatRequest;
import com.caio.ollama_integration.dto.ChatResponse;
import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.exception.OllamaServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private static final String DEFAULT_MODEL = "llama3.2";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final ChatModel chatModel;
    private final OllamaApi ollamaApi;

    public ChatResponse chat(ChatRequest request) {
        validateChatRequest(request);

        log.info("Processando mensagem: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatModel.call(prompt);

            return buildChatResponse(request, aiResponse);
        } catch (Exception e) {
            log.error("Erro ao processar chat: ", e);
            throw new OllamaServiceException("Erro ao comunicar com Ollama: " + e.getMessage(), e);
        }
    }

    public String listModels() {
        try {
            log.info("Listando modelos disponíveis no Ollama");
            ListModelResponse response = ollamaApi.listModels();

            if (response == null || response.models() == null || response.models().isEmpty()) {
                return "Nenhum modelo encontrado no Ollama";
            }

            List<String> modelNames = response.models().stream()
                    .map(model -> String.format("- %s (tamanho: %.2f GB, modificado: %s)",
                            model.name(),
                            model.size() / 1_000_000_000.0,
                            model.modifiedAt()))
                    .collect(Collectors.toList());

            return String.format("Modelos disponíveis no Ollama (%d):\n%s",
                    modelNames.size(),
                    String.join("\n", modelNames));

        } catch (Exception e) {
            log.error("Erro ao listar modelos: ", e);
            throw new OllamaServiceException("Erro ao listar modelos do Ollama: " + e.getMessage(), e);
        }
    }

    private void validateChatRequest(ChatRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request não pode ser nulo");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new InvalidRequestException("Mensagem não pode ser vazia");
        }
        if (request.getMessage().length() > 10000) {
            throw new InvalidRequestException("Mensagem muito longa (máximo 10000 caracteres)");
        }
    }

    private Prompt buildPrompt(ChatRequest request) {
        if (request.getModel() != null || request.getTemperature() != null) {
            OllamaOptions options = OllamaOptions.builder()
                    .withModel(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
                    .withTemperature(request.getTemperature() != null ? request.getTemperature() : DEFAULT_TEMPERATURE)
                    .build();
            return new Prompt(request.getMessage(), options);
        }
        return new Prompt(request.getMessage());
    }

    private ChatResponse buildChatResponse(ChatRequest request,
            org.springframework.ai.chat.model.ChatResponse aiResponse) {
        String responseText = aiResponse.getResult().getOutput().getContent();
        Long tokensUsed = aiResponse.getMetadata() != null
                ? aiResponse.getMetadata().getUsage().getTotalTokens()
                : 0L;

        return ChatResponse.builder()
                .response(responseText)
                .model(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
                .tokensUsed(tokensUsed)
                .build();
    }
}
