package com.caio.ollama_integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String role; // "user" ou "assistant"
    private String content;
    private LocalDateTime timestamp;
    private Integer tokensUsed;

}
