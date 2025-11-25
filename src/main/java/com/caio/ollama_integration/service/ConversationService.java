package com.caio.ollama_integration.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.model.Message;
import com.caio.ollama_integration.model.dto.request.ConversationRequestDTO;
import com.caio.ollama_integration.model.dto.response.ConversationResponseDTO;
import com.caio.ollama_integration.model.dto.response.ModelsListResponseDTO;
import com.caio.ollama_integration.model.mongodb.Conversation;
import com.caio.ollama_integration.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final OllamaService ollamaService;
    private final EmbeddingService embeddingService;

    /**
     * Lista todos os modelos disponíveis no Ollama
     */
    public ModelsListResponseDTO listAllModels() {
        log.info("Buscando todos os modelos disponíveis no Ollama");
        return ollamaService.listModels();
    }

    /**
     * Cria uma nova conversação
     */
    @Transactional
    public Conversation createConversation(String username) {
        log.info("Criando nova conversação para usuário: {}", username);

        Conversation conversation = Conversation.builder()
                .username(username)
                .title("Novo CHAT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Busca conversação por ID e valida se pertence ao usuário
     */
    public Conversation getConversationById(String conversationId, String username) {
        return conversationRepository.findByIdAndUsername(conversationId, username)
                .orElseThrow(
                        () -> new InvalidRequestException("Conversação não encontrada ou não pertence ao usuário"));
    }

    /**
     * Lista todas as conversações de um usuário
     */
    public List<ConversationResponseDTO> listConversations(String username, Boolean activeOnly) {
        log.info("Listando conversações do usuário: {} (ativas: {})", username, activeOnly);

        List<Conversation> conversations = activeOnly != null && activeOnly
                ? conversationRepository.findByUsernameAndActiveOrderByUpdatedAtDesc(username, true)
                : conversationRepository.findByUsernameOrderByUpdatedAtDesc(username);

        return conversations.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Adiciona mensagem à conversação e obtém resposta do LLM com streaming
     * Implementa RAG: busca automaticamente contexto relevante antes de responder
     */
    public Flux<String> chatWithConversationStream(String conversationId, ConversationRequestDTO request,
            String username) {
        log.info("Processando chat com streaming para usuário: {} na conversação: {}", username,
                conversationId);

        if (conversationId == null || conversationId.isBlank()) {
            throw new InvalidRequestException(
                    "conversationId é obrigatório. Crie uma conversação primeiro usando POST /v1/conversations");
        }

        // Busca conversação existente
        Conversation conversation = getConversationById(conversationId, username);
        log.info("Usando conversação existente: {}", conversation.getId());

        // RAG: Busca documentos relevantes para enriquecer o contexto
        String enhancedMessage = buildRAGEnhancedMessage(request.getMessage(), username);

        // Adiciona mensagem do usuário (original, sem o contexto RAG)
        conversation.getMessages().add(Message.builder()
                .role("user")
                .content(request.getMessage())
                .timestamp(LocalDateTime.now())
                .build());

        // Atualiza título se necessário
        if (conversation.getTitle() == null || conversation.getTitle().isBlank() ||
                conversation.getTitle().equals("Novo CHAT")) {
            conversation.setTitle(ollamaService.generateConversationTitle(request.getMessage(), request.getModel()));
        }
        conversation.setModel(request.getModel());
        conversationRepository.save(conversation);

        // Cria referência atômica para acumular a resposta completa
        AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

        // Retorna o streaming puro da resposta (usa a mensagem enriquecida com RAG)
        return ollamaService.chatStream(enhancedMessage, request.getModel(), request.getTemperature())
                .doOnNext(chunk -> fullResponse.get().append(chunk))
                .doOnComplete(() -> {
                    // Salva a resposta completa do assistente
                    Conversation conv = conversationRepository.findById(conversationId)
                            .orElseThrow(() -> new InvalidRequestException("Conversação não encontrada"));

                    String completeResponse = fullResponse.get().toString();
                    Message assistantMessage = Message.builder()
                            .role("assistant")
                            .content(completeResponse)
                            .timestamp(LocalDateTime.now())
                            .tokensUsed(estimateTokens(completeResponse))
                            .build();

                    conv.getMessages().add(assistantMessage);
                    conv.setUpdatedAt(LocalDateTime.now());
                    conversationRepository.save(conv);

                    // Auto-indexa a conversação para buscas futuras
                    indexConversationAsDocument(conv);

                    log.info("Conversação atualizada com streaming: {}", conversationId);
                })
                .doOnError(error -> log.error("Erro no streaming da conversação: ", error));
    }

    /**
     * Constrói mensagem enriquecida com contexto RAG
     * Busca documentos relevantes e adiciona ao prompt
     */
    private String buildRAGEnhancedMessage(String userMessage, String username) {
        try {
            log.debug("Buscando contexto RAG para: {}", userMessage);

            // Busca top 3 documentos mais relevantes
            List<com.caio.ollama_integration.model.mongodb.EmbeddingDocument> relevantDocs = embeddingService
                    .searchSimilarDocuments(userMessage, username, null, 3);

            if (relevantDocs.isEmpty()) {
                log.debug("Nenhum documento relevante encontrado, usando mensagem original");
                return userMessage;
            }

            // Constrói contexto a partir dos documentos encontrados
            StringBuilder context = new StringBuilder();
            context.append("Contexto relevante encontrado:\n\n");

            for (int i = 0; i < relevantDocs.size(); i++) {
                com.caio.ollama_integration.model.mongodb.EmbeddingDocument doc = relevantDocs.get(i);
                context.append(String.format("[Documento %d]\n%s\n\n", i + 1, doc.getContent()));
            }

            context.append("---\n\n");
            context.append("Com base no contexto acima, responda à seguinte pergunta:\n");
            context.append(userMessage);

            log.info("Mensagem enriquecida com {} documentos relevantes", relevantDocs.size());
            return context.toString();

        } catch (Exception e) {
            log.warn("Erro ao buscar contexto RAG, usando mensagem original: {}", e.getMessage());
            return userMessage;
        }
    }

    /**
     * Indexa conversação como documento para buscas semânticas futuras
     * Usa Kafka para processamento distribuído e escalável
     */
    private void indexConversationAsDocument(Conversation conversation) {
        log.debug("[KAFKA] Enfileirando conversação {} para indexação", conversation.getId());

        // Concatena todas as mensagens em um texto
        String conversationText = conversation.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // Metadados da conversação
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("conversationId", conversation.getId());
        metadata.put("title", conversation.getTitle());
        metadata.put("model", conversation.getModel());
        metadata.put("messageCount", conversation.getMessages().size());

        // Enfileira no Kafka para processamento distribuído
                embeddingService.createDocumentAsync(
                conversationText,
                metadata,
                conversation.getUsername(),
                "conversation")
                .thenAccept(eventId -> log.info("[KAFKA] Conversação {} enfileirada - eventId: {}",
                        conversation.getId(), eventId))
                .exceptionally(error -> {
                    log.error("[KAFKA] Erro ao enfileirar conversação {}: {}",
                            conversation.getId(), error.getMessage(), error);
                    return null;
                });
    }

    /**
     * Arquiva (desativa) uma conversação
     */
    @Transactional
    public void archiveConversation(String conversationId, String username) {
        log.info("Arquivando conversação: {} do usuário: {}", conversationId, username);

        Conversation conversation = getConversationById(conversationId, username);
        conversation.setActive(false);
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);
    }

    /**
     * Deleta uma conversação
     */
    @Transactional
    public void deleteConversation(String conversationId, String username) {
        log.info("Deletando conversação: {} do usuário: {}", conversationId, username);

        Conversation conversation = getConversationById(conversationId, username);
        conversationRepository.delete(conversation);
    }

    /**
     * Atualiza o título de uma conversação
     */
    @Transactional
    public ConversationResponseDTO updateConversationTitle(String conversationId, String username, String newTitle) {
        log.info("Atualizando título da conversação: {}", conversationId);

        Conversation conversation = getConversationById(conversationId, username);
        conversation.setTitle(newTitle);
        conversation.setUpdatedAt(LocalDateTime.now());

        Conversation updated = conversationRepository.save(conversation);
        return toResponseDTO(updated);
    }

    /**
     * Busca conversações por modelo
     */
    public List<ConversationResponseDTO> listConversationsByModel(String username, String model) {
        log.info("Listando conversações do usuário: {} com modelo: {}", username, model);

        List<Conversation> conversations = conversationRepository
                .findByUsernameAndModelOrderByUpdatedAtDesc(username, model);

        return conversations.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca conversações semanticamente similares usando embeddings
     */
    public List<ConversationResponseDTO> searchSimilarConversations(String username, String query, int limit) {
        log.info("Buscando conversações similares para usuário: {} com query: {}", username, query);

        // Busca documentos de conversações similares
        List<com.caio.ollama_integration.model.mongodb.EmbeddingDocument> similarDocs = embeddingService
                .searchSimilarDocuments(query, username, "conversation", limit);

        // Extrai IDs de conversações dos documentos
        List<String> conversationIds = similarDocs.stream()
                .map(doc -> (String) doc.getMetadata().get("conversationId"))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // Busca as conversações pelos IDs
        List<Conversation> conversations = conversationRepository.findAllById(conversationIds);

        // Ordena pela ordem dos IDs retornados pela busca semântica
        return conversationIds.stream()
                .map(id -> conversations.stream()
                        .filter(conv -> conv.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(conv -> conv != null)
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Métodos auxiliares

    private ConversationResponseDTO toResponseDTO(Conversation conversation) {
        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .username(conversation.getUsername())
                .title(conversation.getTitle())
                .model(conversation.getModel())
                .messages(conversation.getMessages())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .active(conversation.getActive())
                .build();
    }

    private Integer estimateTokens(String text) {
        // Estimativa simples: ~4 caracteres por token
        return (int) Math.ceil(text.length() / 4.0);
    }

}
