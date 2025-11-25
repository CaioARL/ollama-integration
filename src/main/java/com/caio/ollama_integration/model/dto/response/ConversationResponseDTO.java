package com.caio.ollama_integration.model.dto.response;

import com.caio.ollama_integration.model.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com dados da conversação")
public class ConversationResponseDTO {

    @Schema(description = "ID da conversação", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "Nome do usuário", example = "myapp")
    private String username;

    @Schema(description = "Título da conversação", example = "Sobre Inteligência Artificial")
    private String title;

    @Schema(description = "Modelo LLM usado", example = "llama3.2")
    private String model;

    @Schema(description = "Lista de mensagens da conversação")
    private List<Message> messages;

    @Schema(description = "Data de criação", example = "2024-11-24T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última atualização", example = "2024-11-24T11:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Se a conversação está ativa", example = "true")
    private Boolean active;

}
