package com.caio.ollama_integration.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.caio.ollama_integration.model.dto.request.RAGDocumentRequestDTO;
import com.caio.ollama_integration.model.dto.response.RAGDocumentResponseDTO;
import com.caio.ollama_integration.model.dto.response.RAGSearchResponseDTO;
import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;
import com.caio.ollama_integration.service.EmbeddingService;
import com.caio.ollama_integration.service.RAGService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "Endpoints para gerenciar documentos RAG (Retrieval-Augmented Generation)")
public class RAGController {

    private final EmbeddingService embeddingService;
    private final RAGService ragService;

    @PostMapping("/documents")
    @Operation(summary = "Adicionar documento para RAG", description = "Cria um novo documento com embedding para busca semântica. O documento será usado automaticamente nas conversas.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<RAGDocumentResponseDTO> addDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RAGDocumentRequestDTO request) {

        String username = userDetails.getUsername();
        log.info("Adicionando documento RAG para usuário: {}", username);

        Map<String, Object> metadata = new HashMap<>();
        if (request.getTitle() != null) {
            metadata.put("title", request.getTitle());
        }
        if (request.getSource() != null) {
            metadata.put("source", request.getSource());
        }
        if (request.getTags() != null) {
            metadata.put("tags", request.getTags());
        }

        EmbeddingDocument document = embeddingService.createDocument(
                request.getContent(),
                metadata,
                username,
                request.getDocumentType() != null ? request.getDocumentType() : "knowledge");

        RAGDocumentResponseDTO response = RAGDocumentResponseDTO.builder()
                .id(document.getId())
                .content(document.getContent())
                .documentType(document.getDocumentType())
                .metadata(document.getMetadata())
                .createdAt(document.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents")
    @Operation(summary = "Listar documentos RAG", description = "Lista todos os documentos RAG do usuário autenticado", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<List<RAGDocumentResponseDTO>> listDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String documentType) {

        String username = userDetails.getUsername();
        log.info("Listando documentos RAG para usuário: {}", username);

        List<EmbeddingDocument> documents = documentType != null
                ? embeddingService.searchSimilarDocuments("", username, documentType, 100, 0.0)
                : embeddingService.listUserDocuments(username);

        List<RAGDocumentResponseDTO> response = documents.stream()
                .map(doc -> RAGDocumentResponseDTO.builder()
                        .id(doc.getId())
                        .content(doc.getContent().length() > 200
                                ? doc.getContent().substring(0, 200) + "..."
                                : doc.getContent())
                        .documentType(doc.getDocumentType())
                        .metadata(doc.getMetadata())
                        .createdAt(doc.getCreatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Deletar documento RAG", description = "Remove um documento do sistema RAG", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<Map<String, String>> deleteDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String documentId) {

        String username = userDetails.getUsername();
        log.info("Deletando documento RAG {} para usuário: {}", documentId, username);

        // Verifica se o documento pertence ao usuário
        EmbeddingDocument document = embeddingService.getDocumentById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        if (!document.getUsername().equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "Documento não pertence ao usuário"));
        }

        embeddingService.deleteDocument(documentId);

        return ResponseEntity.ok(Map.of("message", "Documento deletado com sucesso"));
    }

    @PostMapping("/search")
    @Operation(summary = "Buscar documentos similares", description = "Busca semântica avançada com ranking composto (similaridade + recência + relevância)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<RAGSearchResponseDTO> searchDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        String username = userDetails.getUsername();
        log.info("Buscando documentos RAG para query: '{}' (usuário: {})", query, username);

        List<RAGService.RankedDocument> rankedDocs = ragService.retrieveAndRank(query, username, limit);

        List<RAGSearchResponseDTO.RankedDocumentDTO> results = rankedDocs.stream()
                .map(rd -> RAGSearchResponseDTO.RankedDocumentDTO.builder()
                        .id(rd.getDocument().getId())
                        .content(rd.getDocument().getContent())
                        .documentType(rd.getDocument().getDocumentType())
                        .metadata(rd.getDocument().getMetadata())
                        .similarityScore(rd.getSimilarityScore())
                        .recencyScore(rd.getRecencyScore())
                        .relevanceScore(rd.getRelevanceScore())
                        .compositeScore(rd.getCompositeScore())
                        .build())
                .toList();

        RAGSearchResponseDTO response = RAGSearchResponseDTO.builder()
                .query(query)
                .totalResults(results.size())
                .documents(results)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    @Operation(summary = "Testar RAG com preview", description = "Testa o RAG mostrando o prompt enriquecido que seria enviado ao modelo", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<Map<String, Object>> testRAG(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int maxDocs) {

        String username = userDetails.getUsername();
        log.info("Testando RAG para query: '{}' (usuário: {})", query, username);

        String enhancedPrompt = ragService.buildEnhancedPrompt(query, username, maxDocs);
        List<RAGService.RankedDocument> rankedDocs = ragService.retrieveAndRank(query, username, maxDocs);

        Map<String, Object> response = new HashMap<>();
        response.put("originalQuery", query);
        response.put("enhancedPrompt", enhancedPrompt);
        response.put("documentsUsed", rankedDocs.size());
        response.put("documents", rankedDocs.stream()
                .map(rd -> Map.of(
                        "type", rd.getDocument().getDocumentType(),
                        "score", rd.getCompositeScore(),
                        "preview", rd.getDocument().getContent().substring(0,
                                Math.min(100, rd.getDocument().getContent().length())) + "..."))
                .toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/documents/upload")
    @Operation(summary = "Upload de arquivo para RAG", description = "Faz upload de arquivo (TXT, MD) e indexa automaticamente com chunking inteligente", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String source) throws java.io.IOException {

        String username = userDetails.getUsername();
        log.info("Upload de arquivo RAG: {} (usuário: {})", file.getOriginalFilename(), username);

        // Injeta DocumentProcessingService
        var docProcessor = new com.caio.ollama_integration.service.DocumentProcessingService(embeddingService);

        if (!docProcessor.isSupportedFileType(file.getOriginalFilename())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de arquivo não suportado. Use: TXT, MD"));
        }

        Map<String, Object> metadata = new HashMap<>();
        if (title != null)
            metadata.put("title", title);
        if (source != null)
            metadata.put("source", source);

        List<String> documentIds = docProcessor.processAndIndexFile(file, username, metadata);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Arquivo indexado com sucesso");
        response.put("filename", file.getOriginalFilename());
        response.put("chunksCreated", documentIds.size());
        response.put("documentIds", documentIds);

        return ResponseEntity.ok(response);
    }
}
