# Ollama Integration Backend

Backend Spring Boot para integração com Ollama (LLM local) usando Spring AI, com autenticação JWT e documentação Swagger/OpenAPI.

## 📋 Pré-requisitos

- Java 25+
- Maven 3.6+
- Docker e Docker Compose

## 🚀 Como executar

### 1. Subir o Ollama com Docker

```bash
docker-compose up -d
```

Isso irá:
- Iniciar o Ollama na porta `11434`
- Iniciar uma interface web (Open WebUI) na porta `3000` para gerenciar modelos

### 2. Baixar um modelo LLM

Acesse o container do Ollama e baixe um modelo:

```bash
docker exec -it ollama ollama pull llama3.2
```

Outros modelos disponíveis:
- `llama3.2` (recomendado, ~2GB)
- `llama3.2:1b` (menor, ~1.3GB)
- `mistral` (~4GB)
- `codellama` (especializado em código)

Para listar modelos disponíveis:
```bash
docker exec -it ollama ollama list
```

### 3. Compilar e executar a aplicação

```bash
mvn clean install
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

## 📚 Documentação Swagger

Acesse a documentação interativa da API em: `http://localhost:8080//ollama-integration/swagger-ui.html`

## ⚙️ Configuração com .env

O projeto utiliza variáveis de ambiente através do arquivo `.env`. 

### 1. Criar arquivo .env

Copie o arquivo de exemplo e configure suas credenciais:

```bash
cp .env.example .env
```

Edite o `.env` com suas configurações:

```properties
# Configurações do Servidor
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/ollama-integration

# Configurações do Ollama
OLLAMA_BASE_URL=http://localhost:11434

# Configurações JWT
# Gere uma chave secreta forte para produção
JWT_SECRET=your-secret-key-here-change-in-production
JWT_EXPIRATION=3600000

# Credenciais de Autenticação
# IMPORTANTE: Altere estes valores para produção!
AUTH_SUBJECT=myapp
AUTH_ACCESS_KEY=secretkey123
```

⚠️ **Importante**: O arquivo `.env` está no `.gitignore` e **não deve** ser commitado no repositório!

## 🔐 Autenticação JWT

### 1. Obter Token JWT

**POST** `/v1/auth`

```json
{
  "subject": "myapp",
  "accessKey": "secretkey123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "myapp",
  "expiresIn": 3600000
}
```

**Credenciais:**
- Definidas no arquivo `.env` através de `AUTH_SUBJECT` e `AUTH_ACCESS_KEY`
- Por padrão: `subject=myapp` e `accessKey=secretkey123`

### 2. Usar o Token

Adicione o token no header de todas as requisições protegidas:

```
Authorization: Bearer {seu_token}
```

### 3. Validar Token

**POST** `/v1/auth`

```
Authorization: Bearer {seu_token}
```

## 🔌 Endpoints da API

### Chat com o Agente de IA

**POST** `/v1/chat` 🔒 *Requer autenticação*

```json
{
  "message": "Qual é a capital do Brasil?",
  "model": "llama3.2",
  "temperature": 0.7
}
```

**Response:**
```json
{
  "response": "A capital do Brasil é Brasília...",
  "model": "llama3.2",
  "tokensUsed": 150
}
```

### Chat com Streaming (SSE)

**POST** `/v1/chat/stream` 🔒 *Requer autenticação*

```json
{
  "message": "Explique como funciona a inteligência artificial",
  "model": "llama3.2",
  "temperature": 0.7
}
```

**Response:** Server-Sent Events (SSE) - A resposta é enviada em tempo real, token por token.

## 📦 Estrutura do Projeto

```
src/main/java/com/caio/ollama_integration/
├── config/
│   ├── OpenApiConfig.java         # Configuração Swagger/OpenAPI
│   └── WebSecurityConfig.java     # Configuração Spring Security
├── controller/
│   ├── AuthController.java        # Endpoints de autenticação JWT
│   └── ChatController.java        # Endpoints de chat com IA
├── dto/
│   ├── AuthRequest.java           # DTO de login
│   ├── AuthResponse.java          # DTO de resposta JWT
│   ├── ChatRequest.java           # DTO de requisição chat
│   ├── ChatResponse.java          # DTO de resposta chat
│   └── TokenValidationResponse.java  # DTO validação token
├── security/
│   ├── JwtAuthenticationFilter.java  # Filtro de autenticação JWT
│   └── JwtUtil.java               # Utilitário JWT (geração/validação)
├── service/
│   ├── AuthService.java           # Lógica de autenticação
│   └── OllamaService.java         # Lógica de integração com Ollama
├── util/
│   ├── JwtUtil.java               # Utilitário de autenticação
│   └── RequestValidator.java      # Utilitário para validar request
└── OllamaIntegrationApplication.java
```

## 🐳 Gerenciamento do Docker

### Ver logs do Ollama:
```bash
docker logs -f ollama
```

### Parar os serviços:
```bash
docker-compose down
```

### Parar e remover volumes (limpa modelos baixados):
```bash
docker-compose down -v
```

## 🔍 Solução de Problemas

### Erro 401 Unauthorized
- Certifique-se de obter o token JWT primeiro através de `/v1/auth`
- Verifique se o header `Authorization: Bearer {token}` está correto
- Confirme se o token não expirou (validade de 1 hora)

### Ollama não está acessível
- Verifique se o container está rodando: `docker ps`
- Verifique os logs: `docker logs ollama`
- Teste a conexão: `curl http://localhost:11434/tags`

### Modelo não encontrado
- Liste os modelos instalados: `docker exec -it ollama ollama list`
- Baixe o modelo necessário: `docker exec -it ollama ollama pull llama3.2`

### Porta já em uso
- Altere a porta em `docker-compose.yml` ou `application.properties`

## 📚 Recursos

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Swagger/OpenAPI Specification](https://swagger.io/specification/)
- [Ollama Models](https://ollama.ai/library)
- [Open WebUI](https://github.com/open-webui/open-webui)

## 🎯 Próximos Passos

- [x] Adicionar autenticação JWT
- [x] Documentação Swagger/OpenAPI
- [x] Configuração com variáveis de ambiente (.env)
- [x] Autenticação baseada em subject/accessKey
- [x] Adicionar streaming de respostas (SSE)
- [ ] Implementar histórico de conversação com banco de dados
- [ ] Adicionar suporte a embeddings
- [ ] Implementar RAG (Retrieval-Augmented Generation)
- [ ] Sistema de roles e permissões granular
- [ ] Rate limiting por usuário
- [ ] Métricas e monitoramento (Actuator)
- [ ] Rotação automática de access keys
