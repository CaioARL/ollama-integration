package com.caio.ollama_integration.migration;

import com.caio.ollama_integration.hardcode.Role;
import com.caio.ollama_integration.model.User;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@ChangeUnit(id = "create-default-admin-user", order = "001", author = "system")
public class DatabaseChangeLog001 {

    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public DatabaseChangeLog001(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Execution
    public void createDefaultAdminUser() {
        log.info("Verificando se usuário admin padrão existe...");

        Query query = new Query(Criteria.where("username").is("admin"));
        boolean adminExists = mongoTemplate.exists(query, User.class);

        if (adminExists) {
            log.info("Usuário admin já existe. Pulando criação.");
            return;
        }

        log.info("Criando usuário admin padrão...");

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@ollama-integration.com")
                .roles(Set.of(Role.ADMIN, Role.MODERATOR, Role.USER))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        mongoTemplate.save(admin);

        log.info("Usuário admin criado com sucesso!");
        log.info("Credenciais padrão - Username: admin | Password: admin123");
        log.warn("⚠️  IMPORTANTE: Altere a senha padrão em produção!");
    }

    @RollbackExecution
    public void rollback() {
        log.info("Rollback: Removendo usuário admin padrão...");
        Query query = new Query(Criteria.where("username").is("admin"));
        mongoTemplate.remove(query, User.class);
        log.info("Usuário admin removido.");
    }
}
