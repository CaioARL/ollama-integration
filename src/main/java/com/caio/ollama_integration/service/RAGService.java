package com.caio.ollama_integration.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço especializado em Retrieval-Augmented Generation (RAG)
 * Implementa técnicas avançadas de ranking e seleção de contexto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final EmbeddingService embeddingService;

    @Value("${app.similarity.rag-threshold:0.70}")
    private double ragSimilarityThreshold;

    @Value("${app.rag.max-context-tokens:2000}")
    private int maxContextTokens;

    @Value("${app.rag.time-decay-days:30}")
    private int timeDecayDays;

    @Value("${app.rag.diversity-penalty:0.3}")
    private double diversityPenalty;

    /**
     * Busca e ranqueia documentos relevantes com scoring avançado
     * 
     * @param query    Query do usuário
     * @param username Filtro de usuário
     * @param limit    Número máximo de resultados
     * @return Lista de documentos ranqueados com scores compostos
     */
    public List<RankedDocument> retrieveAndRank(String query, String username, int limit) {
        log.info("Iniciando RAG retrieval para query de {} caracteres", query.length());

        // 1. Busca inicial com threshold base (busca mais documentos para re-ranking)
        List<EmbeddingDocument> candidates = embeddingService
                .searchSimilarDocuments(query, username, null, limit * 3, 0);

        if (candidates.isEmpty()) {
            log.debug("Nenhum documento candidato encontrado");
            return List.of();
        }

        // 2. Calcula scores compostos (similaridade + recência + diversidade)
        List<RankedDocument> rankedDocs = candidates.stream()
                .map(doc -> {
                    double similarityScore = (double) doc.getMetadata().getOrDefault("_similarity_score", 0.0);
                    double recencyScore = calculateRecencyScore(doc.getCreatedAt());
                    double relevanceScore = calculateRelevanceScore(doc, query);

                    // Score composto com pesos configuráveis
                    double compositeScore = (similarityScore * 0.5) +
                            (recencyScore * 0.2) +
                            (relevanceScore * 0.3);

                    return RankedDocument.builder()
                            .document(doc)
                            .similarityScore(similarityScore)
                            .recencyScore(recencyScore)
                            .relevanceScore(relevanceScore)
                            .compositeScore(compositeScore)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RankedDocument::getCompositeScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // 3. Aplica penalidade de diversidade (evita documentos muito similares entre
        // si)
        rankedDocs = applyDiversityPenalty(rankedDocs);

        logRetrievalResults(rankedDocs);
        return rankedDocs;
    }

    /**
     * Constrói prompt enriquecido com contexto RAG otimizado
     * Limita tokens e formata contexto de forma estruturada
     */
    public String buildEnhancedPrompt(String userQuery, String username, int maxDocs) {
        List<RankedDocument> rankedDocs = retrieveAndRank(userQuery, username, maxDocs);

        if (rankedDocs.isEmpty()) {
            log.debug("RAG não encontrou contexto relevante, usando query original");
            return userQuery;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("# Contexto Relevante\n\n");
        prompt.append("Use as informações abaixo para responder a pergunta com mais precisão:\n\n");

        int currentTokens = 0;
        int docsIncluded = 0;

        for (RankedDocument rankedDoc : rankedDocs) {
            EmbeddingDocument doc = rankedDoc.getDocument();
            String content = doc.getContent();
            int docTokens = estimateTokens(content);

            // Verifica limite de tokens
            if (currentTokens + docTokens > maxContextTokens) {
                log.debug("Limite de tokens atingido ({}/{}), truncando contexto",
                        currentTokens, maxContextTokens);
                break;
            }

            // Adiciona metadados relevantes
            String source = (String) doc.getMetadata().getOrDefault("source", "Documento");
            String docType = doc.getDocumentType();

            prompt.append(String.format("## [%s] %s (Relevância: %.2f)\n",
                    docType, source, rankedDoc.getCompositeScore()));
            prompt.append(content);
            prompt.append("\n\n---\n\n");

            currentTokens += docTokens;
            docsIncluded++;
        }

        prompt.append("# Pergunta do Usuário\n\n");
        prompt.append(userQuery);
        prompt.append("\n\n**Instruções**: Baseie sua resposta principalmente no contexto fornecido. ");
        prompt.append("Se o contexto não contiver informações suficientes, indique isso claramente.");

        log.info("Prompt RAG construído: {} documentos, ~{} tokens", docsIncluded, currentTokens);
        return prompt.toString();
    }

    /**
     * Calcula score de recência com decaimento temporal
     * Documentos recentes têm score maior
     */
    private double calculateRecencyScore(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0.0;
        }

        long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());

        // Decaimento exponencial: score = e^(-days/decay_period)
        double decayRate = (double) daysSinceCreation / timeDecayDays;
        return Math.exp(-decayRate);
    }

    /**
     * Calcula score de relevância baseado em keywords e metadados
     */
    private double calculateRelevanceScore(EmbeddingDocument doc, String query) {
        String content = doc.getContent().toLowerCase();
        String[] queryTerms = query.toLowerCase().split("\\s+");

        int matchCount = 0;
        for (String term : queryTerms) {
            if (term.length() > 3 && content.contains(term)) {
                matchCount++;
            }
        }

        // Score normalizado (0-1)
        return queryTerms.length > 0 ? (double) matchCount / queryTerms.length : 0.0;
    }

    /**
     * Aplica penalidade de diversidade para evitar documentos redundantes
     * Maximal Marginal Relevance (MMR) simplificado
     */
    private List<RankedDocument> applyDiversityPenalty(List<RankedDocument> rankedDocs) {
        if (rankedDocs.size() <= 1) {
            return rankedDocs;
        }

        List<RankedDocument> diversified = new ArrayList<>();
        diversified.add(rankedDocs.get(0)); // Sempre adiciona o mais relevante

        for (int i = 1; i < rankedDocs.size(); i++) {
            RankedDocument candidate = rankedDocs.get(i);
            double maxSimilarity = 0.0;

            // Calcula similaridade máxima com documentos já selecionados
            for (RankedDocument selected : diversified) {
                double similarity = calculateContentSimilarity(
                        candidate.getDocument().getContent(),
                        selected.getDocument().getContent());
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }

            // Aplica penalidade proporcional à similaridade
            double penalizedScore = candidate.getCompositeScore() * (1.0 - (diversityPenalty * maxSimilarity));
            candidate.setCompositeScore(penalizedScore);
            diversified.add(candidate);
        }

        // Re-ordena após penalização
        diversified.sort(Comparator.comparingDouble(RankedDocument::getCompositeScore).reversed());
        return diversified;
    }

    /**
     * Calcula similaridade simples baseada em overlap de palavras (Jaccard)
     */
    private double calculateContentSimilarity(String content1, String content2) {
        String[] words1 = content1.toLowerCase().split("\\s+");
        String[] words2 = content2.toLowerCase().split("\\s+");

        java.util.Set<String> set1 = java.util.Set.of(words1);
        java.util.Set<String> set2 = java.util.Set.of(words2);

        long intersection = set1.stream().filter(set2::contains).count();
        long union = set1.size() + set2.size() - intersection;

        return union > 0 ? (double) intersection / union : 0.0;
    }

    /**
     * Estima tokens de forma simples (1 palavra ≈ 0.75 tokens)
     */
    private int estimateTokens(String text) {
        return (int) (text.split("\\s+").length * 0.75);
    }

    /**
     * Log estruturado dos resultados de retrieval
     */
    private void logRetrievalResults(List<RankedDocument> rankedDocs) {
        log.info("=== RAG Retrieval Results ===");
        log.info("Total documents retrieved: {}", rankedDocs.size());

        for (int i = 0; i < Math.min(3, rankedDocs.size()); i++) {
            RankedDocument doc = rankedDocs.get(i);
            log.info("  [{}] Type: {}, Composite: {:.3f}, Similarity: {:.3f}, Recency: {:.3f}, Relevance: {:.3f}",
                    i + 1,
                    doc.getDocument().getDocumentType(),
                    doc.getCompositeScore(),
                    doc.getSimilarityScore(),
                    doc.getRecencyScore(),
                    doc.getRelevanceScore());
        }
    }

    /**
     * Inner class para documentos ranqueados
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RankedDocument {
        private EmbeddingDocument document;
        private double similarityScore;
        private double recencyScore;
        private double relevanceScore;
        private double compositeScore;
    }
}
