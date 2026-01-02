package com.caio.ollama_integration.migration;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChangeUnit(id = "create-rag-indexes", order = "002", author = "system")
public class DatabaseChangeLog002 {

    @Execution
    public void createRAGIndexes(MongoTemplate mongoTemplate) {
        log.info("Criando índices para otimização de buscas RAG...");

        // Índice composto para buscas por usuário e tipo de documento
        mongoTemplate.indexOps("documents")
                .createIndex(new Index()
                        .on("username", Sort.Direction.ASC)
                        .on("documentType", Sort.Direction.ASC)
                        .on("active", Sort.Direction.ASC)
                        .named("idx_user_type_active"));

        // Índice para ordenação por recência
        mongoTemplate.indexOps("documents")
                .createIndex(new Index()
                        .on("createdAt", Sort.Direction.DESC)
                        .on("username", Sort.Direction.ASC)
                        .named("idx_recency_user"));

        // Índice para metadados de busca
        mongoTemplate.indexOps("documents")
                .createIndex(new Index()
                        .on("metadata.source", Sort.Direction.ASC)
                        .sparse()
                        .named("idx_metadata_source"));

        mongoTemplate.indexOps("documents")
                .createIndex(new Index()
                        .on("metadata.tags", Sort.Direction.ASC)
                        .sparse()
                        .named("idx_metadata_tags"));

        // Índice para conversações
        mongoTemplate.indexOps("conversations")
                .createIndex(new Index()
                        .on("username", Sort.Direction.ASC)
                        .on("model", Sort.Direction.ASC)
                        .on("active", Sort.Direction.ASC)
                        .named("idx_user_model_active"));

        log.info("Índices criados com sucesso!");
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        log.info("Removendo índices de otimização de buscas RAG...");
        mongoTemplate.indexOps("documents").dropIndex("idx_user_type_active");
        mongoTemplate.indexOps("documents").dropIndex("idx_recency_user");
        mongoTemplate.indexOps("documents").dropIndex("idx_metadata_source");
        mongoTemplate.indexOps("documents").dropIndex("idx_metadata_tags");
        mongoTemplate.indexOps("conversations").dropIndex("idx_user_model_active");
        log.info("Índices removidos com sucesso!");
    }
}
