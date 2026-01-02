package com.caio.ollama_integration.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caio.ollama_integration.exception.InvalidRequestException;
import com.caio.ollama_integration.model.Message;
import com.caio.ollama_integration.model.dto.request.ConversationRequestDTO;
import com.caio.ollama_integration.model.dto.response.ConversationResponseDTO;
import com.caio.ollama_integration.model.dto.response.ModelsListResponseDTO;
import com.caio.ollama_integration.model.mongodb.Conversation;
import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;
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
    private final RAGService ragService;

    @Value("${app.similarity.rag-threshold:0.70}")
    private double ragSimilarityThreshold;

    @Value("${app.similarity.conversation-threshold:0.60}")
    private double conversationSimilarityThreshold;

    @Value("${app.rag.max-documents:5}")
    private int maxRagDocuments;

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
     * Constrói mensagem enriquecida com contexto RAG usando algoritmo avançado
     * Busca documentos relevantes com ranking composto e adiciona ao prompt
     * Aplica threshold de similaridade mínima para evitar contexto irrelevante
     */
    private String buildRAGEnhancedMessage(String userMessage, String username) {
        try {
                log.debug("Buscando contexto RAG avançado para: {}", userMessage);

            // Usa RAGService com ranking avançado (similaridade + recência + relevância)
            String enhancedPrompt = ragService.buildEnhancedPrompt(userMessage, username, maxRagDocuments);

            if (enhancedPrompt.equals(userMessage)) {
                    log.debug("RAG não encontrou contexto relevante, usando mensagem original");
            } else {
                    log.info("Mensagem enriquecida com contexto RAG otimizado");
            }

            return enhancedPrompt;

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
        Map<String, Object> metadata = new HashMap<>();
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

        // Busca documentos de conversações similares (usa threshold do properties)
        List<EmbeddingDocument> similarDocs = embeddingService
                .searchSimilarDocuments(query, username, "conversation", limit, conversationSimilarityThreshold);

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

    /**
     * Estima número de tokens usando algoritmo mais preciso
     * Baseado em padrões de tokenização de modelos GPT:
     * - Palavras comuns: ~0.75 tokens por palavra
     * - Pontuação e espaços: contados separadamente
     * - Números e caracteres especiais: ~1 token cada
     */
    private Integer estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Remove múltiplos espaços
        String normalized = text.replaceAll("\\s+", " ").trim();

        // Conta palavras (sequências alfanuméricas)
        long wordCount = normalized.split("\\s+").length;

        // Conta pontuação e caracteres especiais
        long punctuationCount = normalized.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();

        // Fórmula ajustada:
        // - Palavras: 0.75 tokens por palavra (palavras comuns em português)
        // - Pontuação: 0.5 tokens por caractere (alguns são agrupados)
        // - Adiciona 10% de margem para casos especiais
        double estimatedTokens = (wordCount * 0.75) + (punctuationCount * 0.5);
        int finalEstimate = (int) Math.ceil(estimatedTokens * 1.1);

        log.debug("Estimativa de tokens: {} palavras, {} pontuações = ~{} tokens",
                wordCount, punctuationCount, finalEstimate);

        return Math.max(1, finalEstimate); // Mínimo de 1 token
    }

}
