package com.caio.ollama_integration.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaApi.ListModelResponse;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.dto.ModelResponseDTO;
import com.caio.ollama_integration.dto.ModelsListResponseDTO;
import com.caio.ollama_integration.exception.OllamaServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private static final String DEFAULT_MODEL = "llama3.2";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final ChatModel chatModel;
    private final OllamaApi ollamaApi;

    /**
     * Lista os modelos disponíveis no Ollama
     * 
     * @return ModelsListResponseDTO com informações detalhadas dos modelos
     */
    public ModelsListResponseDTO listModels() {
        try {
            log.info("Listando modelos disponíveis no Ollama");
            ListModelResponse response = ollamaApi.listModels();

            if (response == null || response.models() == null || response.models().isEmpty()) {
                return ModelsListResponseDTO.builder()
                        .totalModels(0)
                        .totalSize(0L)
                        .formattedTotalSize("0 GB")
                        .models(List.of())
                        .build();
            }

            List<ModelResponseDTO> models = response.models().stream()
                    .map(this::mapToModelResponseDTO)
                    .collect(Collectors.toList());

            long totalSize = response.models().stream()
                    .mapToLong(model -> model.size() != null ? model.size() : 0L)
                    .sum();

            return ModelsListResponseDTO.builder()
                    .totalModels(models.size())
                    .totalSize(totalSize)
                    .formattedTotalSize(formatSize(totalSize))
                    .models(models)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao listar modelos: ", e);
            throw new OllamaServiceException("Erro ao listar modelos do Ollama: " + e.getMessage(), e);
        }
    }

    /**
     * Gera um título resumido para a conversação baseado na primeira mensagem
     * 
     * @param firstMessage A primeira mensagem da conversação
     * @param model        Modelo a ser usado
     * @return Título resumido
     */
    public String generateConversationTitle(String firstMessage, String model) {
        try {
            String prompt = String.format(
                    "Crie um título curto e descritivo (máximo 5 palavras) para uma conversa que começa com esta mensagem: \"%s\". Retorne APENAS o título, sem aspas ou explicações.",
                    firstMessage.length() > 100 ? firstMessage.substring(0, 100) + "..." : firstMessage);

            OllamaOptions options = OllamaOptions.builder()
                    .withModel(model != null ? model : DEFAULT_MODEL)
                    .withTemperature(0.3) // Temperatura baixa para respostas mais consistentes
                    .build();

            Prompt titlePrompt = new Prompt(prompt, options);

            String title = chatModel.call(titlePrompt)
                    .getResult()
                    .getOutput()
                    .getContent()
                    .trim();

            // Limita o tamanho do título
            if (title.length() > 60) {
                title = title.substring(0, 57) + "...";
            }

            return title.isEmpty() ? "Nova Conversação" : title;
        } catch (Exception e) {
            log.error("Erro ao gerar título da conversação: ", e);
            return "Nova Conversação";
        }
    }

    /**
     * Método para chat com streaming (SSE)
     * 
     * @param message     Mensagem do usuário
     * @param model       Modelo a ser usado
     * @param temperature Temperatura
     * @return Flux de strings com a resposta em streaming
     */
    public Flux<String> chatStream(String message, String model, Double temperature) {
        try {
            OllamaOptions options = OllamaOptions.builder()
                    .withModel(model != null ? model : DEFAULT_MODEL)
                    .withTemperature(temperature != null ? temperature : DEFAULT_TEMPERATURE)
                    .build();

            Prompt prompt = new Prompt(message, options);

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

    // PRIVATE METHODS

    /**
     * Mapeia Model do OllamaApi para ModelResponseDTO
     */
    private ModelResponseDTO mapToModelResponseDTO(OllamaApi.Model model) {
        ModelResponseDTO.ModelDetailsDTO details = null;

        if (model.details() != null) {
            details = ModelResponseDTO.ModelDetailsDTO.builder()
                    .format(model.details().format())
                    .family(model.details().family())
                    .families(model.details().families())
                    .parameterSize(model.details().parameterSize())
                    .quantizationLevel(model.details().quantizationLevel())
                    .build();
        }

        return ModelResponseDTO.builder()
                .name(model.name())
                .modifiedAt(model.modifiedAt())
                .size(model.size())
                .formattedSize(formatSize(model.size()))
                .digest(model.digest())
                .details(details)
                .build();
    }

    /**
     * Formata tamanho em bytes para formato legível
     */
    private String formatSize(Long sizeInBytes) {
        if (sizeInBytes == null || sizeInBytes == 0) {
            return "0 B";
        }

        double size = sizeInBytes;
        String[] units = { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
