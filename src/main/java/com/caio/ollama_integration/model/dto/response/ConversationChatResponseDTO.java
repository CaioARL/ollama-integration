package com.caio.ollama_integration.model.dto.response;

import com.caio.ollama_integration.model.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta do chat com informações da conversação")
public class ConversationChatResponseDTO {

    @Schema(description = "ID da conversação", example = "507f1f77bcf86cd799439011")
    private String conversationId;

    @Schema(description = "Resposta do modelo", example = "Inteligência artificial é...")
    private String response;

    @Schema(description = "Modelo usado", example = "llama3.2")
    private String model;

    @Schema(description = "Tokens utilizados", example = "150")
    private Integer tokensUsed;

    @Schema(description = "Mensagem do usuário salva")
    private Message userMessage;

    @Schema(description = "Mensagem do assistente salva")
    private Message assistantMessage;

    @Schema(description = "Data da resposta")
    private LocalDateTime timestamp;

}
