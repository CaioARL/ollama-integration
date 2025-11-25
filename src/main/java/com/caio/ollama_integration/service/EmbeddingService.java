package com.caio.ollama_integration.service;

import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;
import com.caio.ollama_integration.repository.DocumentRepository;
import com.caio.ollama_integration.service.kafka.EmbeddingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Serviço para gerenciar embeddings e busca semântica de documentos.
 * Utiliza modelos de embedding para converter texto em vetores.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingProducer embeddingProducer;

    /**
     * Gera embedding vetorial para um texto usando o modelo configurado
     */
    public List<Double> generateEmbedding(String text) {
        try {
            log.debug("Gerando embedding para texto de {} caracteres", text.length());
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

            float[] embeddingArray = response.getResults().get(0).getOutput();
            return IntStream.range(0, embeddingArray.length)
                    .mapToDouble(i -> (double) embeddingArray[i])
                    .boxed()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erro ao gerar embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar embedding", e);
        }
    }

    /**
     * Cria e salva um novo documento com embedding
     */
    public EmbeddingDocument createDocument(String content, Map<String, Object> metadata,
            String username, String documentType) {
        log.info("Criando documento tipo '{}' para usuário '{}'", documentType, username);

        List<Double> embedding = generateEmbedding(content);

        EmbeddingDocument document = EmbeddingDocument.builder()
                .content(content)
                .embedding(embedding)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .username(username)
                .documentType(documentType)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(document);
    }

    /**
     * Enfileira documento para processamento assíncrono via Kafka
     * Escalável horizontalmente - múltiplas instâncias podem consumir
     * 
     * @return CompletableFuture com o ID do evento Kafka
     */
    public CompletableFuture<String> createDocumentAsync(String content, Map<String, Object> metadata,
            String username, String documentType) {
        log.info("[KAFKA] Enfileirando embedding para usuário '{}', tipo '{}'", username, documentType);
        return embeddingProducer.sendEmbeddingRequest(content, metadata, username, documentType);
    }

    /**
     * Busca documentos semanticamente similares usando similaridade cosseno
     * 
     * @param query        Texto da consulta
     * @param username     Filtrar por usuário (null para buscar em todos)
     * @param documentType Filtrar por tipo (null para buscar em todos)
     * @param limit        Número máximo de resultados
     * @return Lista de documentos ordenados por similaridade (maior primeiro)
     */
    public List<EmbeddingDocument> searchSimilarDocuments(String query, String username,
            String documentType, int limit) {
        log.info("Buscando documentos similares para query de {} caracteres", query.length());

        // Gera embedding da query
        List<Double> queryEmbedding = generateEmbedding(query);

        // Busca documentos candidatos
        List<EmbeddingDocument> candidates;
        if (username != null && documentType != null) {
            candidates = documentRepository.findByUsernameAndDocumentTypeAndActiveTrue(username, documentType);
        } else if (username != null) {
            candidates = documentRepository.findByUsernameAndActiveTrue(username);
        } else if (documentType != null) {
            candidates = documentRepository.findByDocumentTypeAndActiveTrue(documentType);
        } else {
            candidates = documentRepository.findByActiveTrue();
        }

        if (candidates.isEmpty()) {
            log.debug("Nenhum documento candidato encontrado");
            return Collections.emptyList();
        }

        // Calcula similaridade cosseno e ordena
        return candidates.stream()
                .map(doc -> {
                    double similarity = cosineSimilarity(queryEmbedding, doc.getEmbedding());
                    // Adiciona score aos metadados temporariamente
                    Map<String, Object> metaWithScore = new HashMap<>(doc.getMetadata());
                    metaWithScore.put("_similarity_score", similarity);
                    doc.setMetadata(metaWithScore);
                    return doc;
                })
                .sorted((d1, d2) -> {
                    double score1 = (double) d1.getMetadata().get("_similarity_score");
                    double score2 = (double) d2.getMetadata().get("_similarity_score");
                    return Double.compare(score2, score1); // Ordem decrescente
                })
                .limit(limit)
                .collect(Collectors.toList());
    }


    /**
     * Calcula similaridade cosseno entre dois vetores
     */
    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vetores devem ter o mesmo tamanho");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += Math.pow(vec1.get(i), 2);
            norm2 += Math.pow(vec2.get(i), 2);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Lista todos os documentos de um usuário
     */
    public List<EmbeddingDocument> listUserDocuments(String username) {
        return documentRepository.findByUsernameAndActiveTrue(username);
    }

    /**
     * Lista documentos por tipo
     */
    public List<EmbeddingDocument> listDocumentsByType(String documentType) {
        return documentRepository.findByDocumentTypeAndActiveTrue(documentType);
    }

    /**
     * Busca documento por ID
     */
    public Optional<EmbeddingDocument> getDocumentById(String id) {
        return documentRepository.findById(id);
    }

    /**
     * Deleta um documento
     */
    public void deleteDocument(String id) {
        documentRepository.deleteById(id);
        log.info("Documento {} deletado", id);
    }

    /**
     * Atualiza o conteúdo de um documento e regenera o embedding
     */
    public EmbeddingDocument updateDocument(String id, String newContent, Map<String, Object> newMetadata) {
        EmbeddingDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado: " + id));

        if (newContent != null && !newContent.equals(document.getContent())) {
            document.setContent(newContent);
            document.setEmbedding(generateEmbedding(newContent));
        }

        if (newMetadata != null) {
            document.setMetadata(newMetadata);
        }

        document.setUpdatedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }
}
