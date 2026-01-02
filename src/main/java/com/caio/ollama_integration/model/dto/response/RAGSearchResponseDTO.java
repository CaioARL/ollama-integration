package com.caio.ollama_integration.model.dto.response;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response de busca RAG com ranking")
public class RAGSearchResponseDTO {

    @Schema(description = "Query original da busca")
    private String query;

    @Schema(description = "Total de documentos encontrados")
    private int totalResults;

    @Schema(description = "Documentos ranqueados por relevância")
    private List<RankedDocumentDTO> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Documento ranqueado com scores detalhados")
    public static class RankedDocumentDTO {

        @Schema(description = "ID do documento")
        private String id;

        @Schema(description = "Conteúdo do documento")
        private String content;

        @Schema(description = "Tipo do documento")
        private String documentType;

        @Schema(description = "Metadados do documento")
        private Map<String, Object> metadata;

        @Schema(description = "Score de similaridade vetorial (0.0-1.0)", example = "0.85")
        private double similarityScore;

        @Schema(description = "Score de recência temporal (0.0-1.0)", example = "0.92")
        private double recencyScore;

        @Schema(description = "Score de relevância por keywords (0.0-1.0)", example = "0.75")
        private double relevanceScore;

        @Schema(description = "Score composto final (0.0-1.0)", example = "0.84")
        private double compositeScore;
    }
}
