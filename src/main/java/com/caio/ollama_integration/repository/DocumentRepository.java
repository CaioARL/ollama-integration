package com.caio.ollama_integration.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.caio.ollama_integration.model.mongodb.EmbeddingDocument;

import java.util.List;

/**
 * Repository para operações de banco de dados em documentos com embeddings.
 * Suporta busca semântica vetorial.
 */
@Repository
public interface DocumentRepository extends MongoRepository<EmbeddingDocument, String> {

    /**
     * Busca documentos por username e tipo
     */
    List<EmbeddingDocument> findByUsernameAndDocumentTypeAndActiveTrue(String username, String documentType);

    /**
     * Busca documentos ativos por username
     */
    List<EmbeddingDocument> findByUsernameAndActiveTrue(String username);

    /**
     * Busca documentos por tipo
     */
    List<EmbeddingDocument> findByDocumentTypeAndActiveTrue(String documentType);

    /**
     * Busca todos os documentos ativos
     */
    List<EmbeddingDocument> findByActiveTrue();
}
