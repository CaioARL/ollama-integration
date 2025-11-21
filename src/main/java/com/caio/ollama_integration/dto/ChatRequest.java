package com.caio.ollama_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String model; // opcional, usa o padrão se não informado
    private Double temperature; // opcional
}
