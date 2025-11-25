package com.caio.ollama_integration.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.caio.ollama_integration.model.mongodb.Conversation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    List<Conversation> findByUsernameOrderByUpdatedAtDesc(String username);

    List<Conversation> findByUsernameAndActiveOrderByUpdatedAtDesc(String username, Boolean active);

    Optional<Conversation> findByIdAndUsername(String id, String username);

    List<Conversation> findByUsernameAndModelOrderByUpdatedAtDesc(String username, String model);

}
