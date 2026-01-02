package com.caio.ollama_integration.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço para processar documentos para RAG
 * Suporta TXT, MD e futuramente PDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final EmbeddingService embeddingService;

    private static final int CHUNK_SIZE = 1000; // caracteres por chunk
    private static final int CHUNK_OVERLAP = 200; // overlap entre chunks

    /**
     * Processa arquivo e cria documentos RAG com chunking
     */
    public List<String> processAndIndexFile(MultipartFile file, String username, Map<String, Object> metadata)
            throws IOException {

        String filename = file.getOriginalFilename();
        String extension = getFileExtension(filename);

        log.info("Processando arquivo: {} (tipo: {}, tamanho: {} bytes)",
                filename, extension, file.getSize());

        String content = extractContent(file, extension);

        // Adiciona metadados do arquivo
        Map<String, Object> enrichedMetadata = new HashMap<>(metadata);
        enrichedMetadata.put("filename", filename);
        enrichedMetadata.put("fileType", extension);
        enrichedMetadata.put("originalSize", file.getSize());

        // Divide em chunks se o conteúdo for muito grande
        List<String> chunks = splitIntoChunks(content);
        List<String> documentIds = new ArrayList<>();

        log.info("Arquivo dividido em {} chunks", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            Map<String, Object> chunkMetadata = new HashMap<>(enrichedMetadata);
            chunkMetadata.put("chunkIndex", i);
            chunkMetadata.put("totalChunks", chunks.size());
            chunkMetadata.put("source", filename + " (parte " + (i + 1) + "/" + chunks.size() + ")");

            var document = embeddingService.createDocument(
                    chunk,
                    chunkMetadata,
                    username,
                    "document");

            documentIds.add(document.getId());
        }

        log.info("Arquivo indexado com sucesso: {} documentos criados", documentIds.size());
        return documentIds;
    }

    /**
     * Extrai conteúdo textual do arquivo baseado na extensão
     */
    private String extractContent(MultipartFile file, String extension) throws IOException {
        return switch (extension.toLowerCase()) {
            case "txt", "md", "markdown" -> extractTextContent(file);
            case "pdf" -> extractPdfContent(file);
            default -> throw new IllegalArgumentException("Tipo de arquivo não suportado: " + extension);
        };
    }

    /**
     * Extrai conteúdo de arquivo de texto
     */
    private String extractTextContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * Extrai conteúdo de PDF (placeholder - requer biblioteca externa)
     * TODO: Adicionar dependência Apache PDFBox ou similar
     */
    private String extractPdfContent(MultipartFile file) throws IOException {
        log.warn("Extração de PDF não implementada ainda");
        throw new UnsupportedOperationException(
                "Extração de PDF requer biblioteca adicional (Apache PDFBox). " +
                        "Por enquanto, use arquivos TXT ou Markdown.");
    }

    /**
     * Divide texto em chunks com overlap para manter contexto
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();

        if (content.length() <= CHUNK_SIZE) {
            chunks.add(content);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());

            // Tenta quebrar em ponto natural (fim de frase)
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                if (breakPoint > start && breakPoint < end) {
                    end = breakPoint + 1;
                }
            }

            chunks.add(content.substring(start, end).trim());
            start = end - CHUNK_OVERLAP; // Overlap para manter contexto

            if (start < 0)
                start = 0;
        }

        return chunks;
    }

    /**
     * Extrai extensão do arquivo
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Valida se o tipo de arquivo é suportado
     */
    public boolean isSupportedFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return List.of("txt", "md", "markdown", "pdf").contains(extension);
    }
}
