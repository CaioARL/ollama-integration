package com.caio.ollama_integration.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Representa um documento armazenado com embedding vetorial para busca semântica.
 * Usado para implementar RAG (Retrieval-Augmented Generation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class EmbeddingDocument {
    
    @Id
    private String id;
    
    /**
     * Conteúdo textual do documento
     */
    private String content;
    
    /**
     * Embedding vetorial do conteúdo (gerado por modelo de embeddings)
     */
    private List<Double> embedding;
    
    /**
     * Metadados do documento (ex: título, fonte, tags, etc.)
     */
    private Map<String, Object> metadata;
    
    /**
     * Username do proprietário do documento
     */
    @Indexed
    private String username;
    
    /**
     * Tipo/categoria do documento (ex: "conversation", "knowledge_base", "file")
     */
    @Indexed
    private String documentType;
    
    /**
     * Se o documento está ativo e disponível para busca
     */
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Data de criação do documento
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Data da última atualização
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
