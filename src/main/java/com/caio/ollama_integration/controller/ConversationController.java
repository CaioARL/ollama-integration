package com.caio.ollama_integration.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.caio.ollama_integration.model.dto.request.ConversationRequestDTO;
import com.caio.ollama_integration.model.dto.response.ConversationResponseDTO;
import com.caio.ollama_integration.model.dto.response.ModelsListResponseDTO;
import com.caio.ollama_integration.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("v1/conversations")
@Tag(name = "Conversações", description = "Gerenciamento de histórico de conversações com IA")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/models")
    @Operation(summary = "Listar todos os modelos disponíveis", description = "Retorna uma lista detalhada de todos os modelos disponíveis no Ollama com suas propriedades")
    public ResponseEntity<ModelsListResponseDTO> listAllModels() {
        log.info("Listando todos os modelos disponíveis no Ollama");

        return ResponseEntity.ok(conversationService.listAllModels());
    }

    @PostMapping
    @Operation(summary = "Criar nova conversação", description = "Cria uma nova conversação vazia com o modelo especificado")
    public ResponseEntity<ConversationResponseDTO> createConversation(@AuthenticationPrincipal String username) {
        log.info("Criando nova conversação para usuário: {}", username);

        var conversation = conversationService.createConversation(username);
        return ResponseEntity.ok(ConversationResponseDTO.builder()
                .id(conversation.getId())
                .username(conversation.getUsername())
                .title(conversation.getTitle())
                .model(conversation.getModel())
                .messages(conversation.getMessages())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .active(conversation.getActive())
                .build());
    }

    @PostMapping("/{conversationId}/chat")
    @Operation(summary = "Enviar mensagem em conversação (Streaming SSE)", description = "Envia uma mensagem para uma conversação existente e retorna a resposta em tempo real via Server-Sent Events")
    public Flux<String> chatInConversation(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @Valid @RequestBody ConversationRequestDTO request,
            @AuthenticationPrincipal String username) {
        log.info("Chat request recebido na conversação: {} do usuário: {}", conversationId, username);

        return conversationService.chatWithConversationStream(conversationId, request, username);
    }

    @GetMapping
    @Operation(summary = "Listar conversações", description = "Lista todas as conversações do usuário autenticado")
    public ResponseEntity<List<ConversationResponseDTO>> listConversations(
            @Parameter(description = "Filtrar apenas conversações ativas") @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal String username) {
        log.info("Listando conversações do usuário: {}", username);

        return ResponseEntity.ok(conversationService.listConversations(username, active));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Obter conversação específica", description = "Retorna os detalhes completos de uma conversação, incluindo todas as mensagens")
    public ResponseEntity<ConversationResponseDTO> getConversation(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @AuthenticationPrincipal String username) {
        log.info("Buscando conversação: {} do usuário: {}", conversationId, username);

        var conversation = conversationService.getConversationById(conversationId, username);
        return ResponseEntity.ok(ConversationResponseDTO.builder()
                .id(conversation.getId())
                .username(conversation.getUsername())
                .title(conversation.getTitle())
                .model(conversation.getModel())
                .messages(conversation.getMessages())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .active(conversation.getActive())
                .build());
    }

    @GetMapping("/model/{model}")
    @Operation(summary = "Listar conversações por modelo", description = "Lista todas as conversações que usaram um modelo específico")
    public ResponseEntity<List<ConversationResponseDTO>> listConversationsByModel(
            @Parameter(description = "Nome do modelo (ex: llama3.2)") @PathVariable String model,
            @AuthenticationPrincipal String username) {
        log.info("Listando conversações do modelo: {} do usuário: {}", model, username);

        return ResponseEntity.ok(conversationService.listConversationsByModel(username, model));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar conversações semanticamente", description = "Busca conversações similares à query usando embeddings vetoriais")
    public ResponseEntity<List<ConversationResponseDTO>> searchConversations(
            @Parameter(description = "Texto da busca semântica") @RequestParam String query,
            @Parameter(description = "Número máximo de resultados") @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal String username) {
        log.info("Busca semântica de conversações para usuário: {} com query: {}", username, query);

        return ResponseEntity.ok(conversationService.searchSimilarConversations(username, query, limit));
    }

    @PatchMapping("/{conversationId}/title")
    @Operation(summary = "Atualizar título da conversação", description = "Atualiza o título de uma conversação existente")
    public ResponseEntity<ConversationResponseDTO> updateTitle(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @RequestParam String newTitle,
            @AuthenticationPrincipal String username) {
        log.info("Atualizando título da conversação: {}", conversationId);

        return ResponseEntity.ok(conversationService.updateConversationTitle(
                conversationId, username, newTitle));
    }

    @PatchMapping("/{conversationId}/archive")
    @Operation(summary = "Arquivar conversação", description = "Marca uma conversação como inativa (arquivada)")
    public ResponseEntity<Void> archiveConversation(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @AuthenticationPrincipal String username) {
        log.info("Arquivando conversação: {}", conversationId);

        conversationService.archiveConversation(conversationId, username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Deletar conversação", description = "Remove permanentemente uma conversação e todo seu histórico")
    public ResponseEntity<Void> deleteConversation(
            @Parameter(description = "ID da conversação") @PathVariable String conversationId,
            @AuthenticationPrincipal String username) {
        log.info("Deletando conversação: {}", conversationId);

        conversationService.deleteConversation(conversationId, username);
        return ResponseEntity.noContent().build();
    }

}
