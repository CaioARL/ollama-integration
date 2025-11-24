package com.caio.ollama_integration.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaApi.ListModelResponse;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.dto.ChatRequestDTO;
import com.caio.ollama_integration.dto.ChatResponseDTO;
import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.exception.OllamaServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private static final String DEFAULT_MODEL = "llama3.1";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final ChatModel chatModel;
    private final OllamaApi ollamaApi;

    public ChatResponseDTO chat(ChatRequestDTO request) {
        validateChatRequest(request);

        log.info("Processando mensagem: {}", request.getMessage());

        try {
            return buildChatResponse(request, chatModel.call(buildPrompt(request)));
        } catch (Exception e) {
            log.error("Erro ao processar chat: ", e);
            throw new OllamaServiceException("Erro ao comunicar com Ollama: " + e.getMessage(), e);
        }
    }

    public Flux<String> chatStream(ChatRequestDTO request) {
        validateChatRequest(request);

        log.info("Processando mensagem com streaming: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);

            return chatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null &&
                                response.getResult().getOutput() != null &&
                                response.getResult().getOutput().getContent() != null) {
                            return response.getResult().getOutput().getContent();
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty())
                    .doOnComplete(() -> log.info("Streaming completado"))
                    .doOnError(error -> log.error("Erro no streaming: ", error));
        } catch (Exception e) {
            log.error("Erro ao iniciar streaming: ", e);
            return Flux.error(new OllamaServiceException("Erro ao comunicar com Ollama: " + e.getMessage(), e));
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

    // Private Methods
    private void validateChatRequest(ChatRequestDTO request) {
        if (request == null) {
            throw new InvalidRequestException("Request não pode ser nulo");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new InvalidRequestException("Mensagem não pode ser vazia");
        }
        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            request.setModel(DEFAULT_MODEL);
        }
        if (request.getTemperature() == null) {
            request.setTemperature(DEFAULT_TEMPERATURE);
        }
        if (request.getMessage().length() > 10000) {
            throw new InvalidRequestException("Mensagem muito longa (máximo 10000 caracteres)");
        }
    }

    private Prompt buildPrompt(ChatRequestDTO request) {
        if (request.getModel() != null || request.getTemperature() != null) {
            OllamaOptions options = OllamaOptions.builder()
                    .withModel(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
                    .withTemperature(request.getTemperature() != null ? request.getTemperature() : DEFAULT_TEMPERATURE)
                    .build();
            return new Prompt(request.getMessage(), options);
        }
        return new Prompt(request.getMessage());
    }

    private ChatResponseDTO buildChatResponse(ChatRequestDTO request, ChatResponse aiResponse) {
        String responseText = aiResponse.getResult().getOutput().getContent();
        Long tokensUsed = aiResponse.getMetadata() != null
                ? aiResponse.getMetadata().getUsage().getTotalTokens()
                : 0L;

        return ChatResponseDTO.builder()
                .response(responseText)
                .model(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
                .tokensUsed(tokensUsed)
                .build();
    }
}
