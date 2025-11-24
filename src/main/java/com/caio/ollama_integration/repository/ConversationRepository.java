package com.caio.ollama_integration.repository;

import com.caio.ollama_integration.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // Buscar todas as conversações de um usuário
    List<Conversation> findByUsernameOrderByUpdatedAtDesc(String username);

    // Buscar conversações ativas de um usuário
    List<Conversation> findByUsernameAndActiveOrderByUpdatedAtDesc(String username, Boolean active);

    // Buscar conversação específica de um usuário
    Optional<Conversation> findByIdAndUsername(String id, String username);

    // Buscar por modelo usado
    List<Conversation> findByUsernameAndModelOrderByUpdatedAtDesc(String username, String model);

}
