package com.caio.ollama_integration.model;

public enum Role {
    USER, // Usuário comum - pode usar chat e gerenciar suas próprias conversações
    ADMIN, // Administrador - pode gerenciar usuários e acessar todas as conversações
    MODERATOR // Moderador - pode visualizar conversações de outros usuários mas não deletar
}
