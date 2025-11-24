package com.caio.ollama_integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    private String username; // Usuário autenticado (subject do JWT)

    private String title; // Título da conversação (opcional)

    private String model; // Modelo usado (llama3.2, etc)

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean active = true; // Conversação ativa ou arquivada

}
