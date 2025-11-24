package com.caio.ollama_integration;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class OllamaIntegrationApplication {

	@PostConstruct
	public void init() {
		// Define timezone padrão da aplicação para horário de Brasília
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
	}

	public static void main(String[] args) {
		SpringApplication.run(OllamaIntegrationApplication.class, args);
	}

}
