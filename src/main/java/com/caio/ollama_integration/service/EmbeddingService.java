package com.caio.ollama_integration.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;
import com.caio.ollama_integration.repository.DocumentRepository;
import com.caio.ollama_integration.service.kafka.EmbeddingProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingProducer embeddingProducer;

    @Value("${app.similarity.default-threshold:0.60}")
    private double defaultSimilarityThreshold;

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
     * @param query         Texto da consulta
     * @param username      Filtrar por usuário (null para buscar em todos)
     * @param documentType  Filtrar por tipo (null para buscar em todos)
     * @param limit         Número máximo de resultados
     * @param minSimilarity Threshold mínimo de similaridade (0.0 a 1.0)
     * @return Lista de documentos ordenados por similaridade (maior primeiro)
     */
    public List<EmbeddingDocument> searchSimilarDocuments(String query, String username,
            String documentType, int limit, double minSimilarity) {
        log.info("Buscando documentos similares para query de {} caracteres (threshold: {})",
                query.length(), minSimilarity);

        // Gera e normaliza embedding da query
        List<Double> queryEmbedding = normalizeEmbedding(generateEmbedding(query));

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

        log.debug("Avaliando {} documentos candidatos", candidates.size());

        // Calcula similaridade cosseno, filtra por threshold e ordena
        List<EmbeddingDocument> results = candidates.stream()
                .map(doc -> {
                    // Normaliza embedding do documento para melhor comparação
                    List<Double> normalizedDocEmbedding = normalizeEmbedding(doc.getEmbedding());
                    double similarity = cosineSimilarity(queryEmbedding, normalizedDocEmbedding);

                    // Adiciona score aos metadados temporariamente
                    Map<String, Object> metaWithScore = new HashMap<>(doc.getMetadata());
                    metaWithScore.put("_similarity_score", similarity);
                    doc.setMetadata(metaWithScore);
                    return doc;
                })
                .filter(doc -> {
                    double score = (double) doc.getMetadata().get("_similarity_score");
                    return score >= minSimilarity; // Aplica threshold
                })
                .sorted((d1, d2) -> {
                    double score1 = (double) d1.getMetadata().get("_similarity_score");
                    double score2 = (double) d2.getMetadata().get("_similarity_score");
                    return Double.compare(score2, score1); // Ordem decrescente
                })
                .limit(limit)
                .collect(Collectors.toList());

        // Log dos resultados filtrados
        if (results.isEmpty()) {
            log.info("Nenhum documento passou o threshold de similaridade mínima: {}", minSimilarity);
        } else {
            log.info("Encontrados {} documentos relevantes (de {} candidatos)", results.size(), candidates.size());
            results.forEach(doc -> {
                double score = (double) doc.getMetadata().get("_similarity_score");
                String docType = doc.getDocumentType();
                String preview = doc.getContent().substring(0, Math.min(50, doc.getContent().length()));
                log.debug("  → [{}] Score: {:.4f} - {}", docType, score, preview + "...");
            });
        }

        return results;
    }

    /**
     * Sobrecarga para manter compatibilidade (usa threshold configurável do
     * properties)
     */
    public List<EmbeddingDocument> searchSimilarDocuments(String query, String username,
            String documentType, int limit) {
        return searchSimilarDocuments(query, username, documentType, limit, defaultSimilarityThreshold);
    }

    /**
     * Normaliza um vetor de embedding para ter magnitude 1
     * Melhora a precisão do cálculo de similaridade cosseno
     */
    private List<Double> normalizeEmbedding(List<Double> embedding) {
        // Calcula a magnitude do vetor
        double magnitude = Math.sqrt(embedding.stream()
                .mapToDouble(val -> val * val)
                .sum());

        // Evita divisão por zero
        if (magnitude == 0.0) {
            log.warn("Embedding com magnitude zero detectado, retornando original");
            return embedding;
        }

        // Normaliza cada componente
        return embedding.stream()
                .map(val -> val / magnitude)
                .collect(Collectors.toList());
    }

    /**
     * Calcula similaridade cosseno entre dois vetores (assumindo já normalizados)
     * Retorna valor entre -1 e 1, onde 1 = idênticos, 0 = ortogonais, -1 = opostos
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
