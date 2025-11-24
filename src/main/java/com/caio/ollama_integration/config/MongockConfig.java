package com.caio.ollama_integration.config;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMongock
public class MongockConfig {
    // Mongock será configurado automaticamente através do application.properties
    // e escaneará o pacote com.caio.ollama_integration.migration
}
