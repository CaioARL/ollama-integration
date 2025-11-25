package com.caio.ollama_integration.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String LICENSE_NAME = "";
    private static final String LICENSE_URL = "";
    private static final String API_TITLE = "Ollama Integration API";
    private static final String API_DESCRIPTION = "API REST para integração com Ollama LLM usando Spring AI.";
    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    private final BuildProperties buildProperties;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder().group("v1")
                .pathsToMatch("/v1/**")
                .addOpenApiCustomizer(openApi -> openApi.info(createInfo(this.buildProperties.getVersion())))
                .build();
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(buildComponents())
                .externalDocs(buildExternalDocumentation());
    }

    private Info buildApiInfo() {
        return new Info()
                .title(API_TITLE)
                .version(buildProperties.getVersion())
                .description(buildDescription())
                .license(buildLicense());
    }

    private String buildDescription() {
        return API_DESCRIPTION;
    }

    private License buildLicense() {
        return new License()
                .name("Apache License 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");
    }

    private List<Server> buildServers() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Servidor de Desenvolvimento Local");

        return List.of(localServer);
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, buildSecurityScheme());
    }

    private SecurityScheme buildSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Autenticação via JWT Bearer Token.");
    }

    private ExternalDocumentation buildExternalDocumentation() {
        return new ExternalDocumentation()
                .description("Documentação do Spring AI")
                .url("https://docs.spring.io/spring-ai/reference/");
    }

    /*
     * PRIVATE METHODS
     */
    private Info createInfo(String version) {
        return new Info().title(API_TITLE).version(version).description(API_DESCRIPTION)
                .license(new License().name(LICENSE_NAME).url(LICENSE_URL));
    }
}
