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
- Iniciar o MongoDB na porta `27017`
- Configurar automaticamente o banco de dados para histórico de conversações

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

# Configurações MongoDB
MONGODB_URI=mongodb://admin:admin123@localhost:27017/ollama_chat?authSource=admin
MONGODB_DATABASE=ollama_chat

# Configurações JWT
# Gere uma chave secreta forte para produção
JWT_SECRET=your-secret-key-here-change-in-production
JWT_EXPIRATION=3600000
```

⚠️ **Importante**: O arquivo `.env` está no `.gitignore` e **não deve** ser commitado no repositório!

## 🔐 Sistema de Roles e Permissões

A aplicação possui três níveis de permissão:

### USER (Usuário Comum)
- Usar chat e conversar com IA
- Gerenciar suas próprias conversações
- Visualizar histórico de suas conversas

### MODERATOR (Moderador)
- Todas as permissões de USER
- Visualizar conversações de outros usuários
- Listar todos os usuários do sistema

### ADMIN (Administrador)
- Todas as permissões de MODERATOR
- Criar novos usuários
- Atualizar roles de usuários
- Desativar usuários
- Deletar qualquer conversação

## 🔐 Autenticação JWT

### 1. Obter Token JWT

**POST** `/v1/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["USER", "ADMIN"],
  "expiresIn": 3600000
}
```

**Nota:** Para criar o primeiro usuário administrador, use o endpoint `/v1/admin/users` (veja seção de Administração).

### 2. Usar o Token

Adicione o token no header de todas as requisições protegidas:

```
Authorization: Bearer {seu_token}
```

### 3. Validar Token

**GET** `/v1/auth/validate` 🔒 *Requer autenticação*

```
Authorization: Bearer {seu_token}
```

## 🔌 Endpoints da API

### Chat com Histórico de Conversação

**POST** `/v1/conversations/chat` 🔒 *Requer autenticação*

```json
{
  "conversationId": "507f1f77bcf86cd799439011",
  "message": "Continue nossa conversa sobre IA",
  "model": "llama3.2",
  "temperature": 0.7,
  "title": "Discussão sobre IA"
}
```

**Response:**
```json
{
  "conversationId": "507f1f77bcf86cd799439011",
  "response": "Claro! Continuando nossa discussão...",
  "model": "llama3.2",
  "tokensUsed": 150,
  "timestamp": "2024-11-24T10:30:00"
}
```

**Nota:** Se não fornecer `conversationId`, uma nova conversação será criada automaticamente.

### Listar Conversações

**GET** `/v1/conversations` 🔒 *Requer autenticação*

Retorna todas as conversações do usuário autenticado.

**GET** `/v1/conversations?active=true` - Apenas conversações ativas

### Obter Conversação Específica

**GET** `/v1/conversations/{conversationId}` 🔒 *Requer autenticação*

Retorna detalhes completos de uma conversação, incluindo todo o histórico de mensagens.

### Atualizar Título da Conversação

**PATCH** `/v1/conversations/{conversationId}/title` 🔒 *Requer autenticação*

```json
{
  "title": "Novo título da conversa"
}
```

### Arquivar Conversação

**PATCH** `/v1/conversations/{conversationId}/archive` 🔒 *Requer autenticação*

Marca a conversação como inativa (arquivada).

### Deletar Conversação

**DELETE** `/v1/conversations/{conversationId}` 🔒 *Requer autenticação*

Remove permanentemente a conversação e todo seu histórico.

### Endpoints de Administração

#### Criar Novo Usuário
**POST** `/v1/admin/users` 🔒 *Requer ADMIN*

```json
{
  "username": "johndoe",
  "password": "senha123",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

#### Listar Todos os Usuários
**GET** `/v1/admin/users` 🔒 *Requer ADMIN ou MODERATOR*

#### Atualizar Roles de Usuário
**PATCH** `/v1/admin/users/{username}/roles` 🔒 *Requer ADMIN*

```json
{
  "roles": ["USER", "MODERATOR"]
}
```

#### Desativar Usuário
**DELETE** `/v1/admin/users/{username}` 🔒 *Requer ADMIN*

#### Listar Conversações de Usuário Específico
**GET** `/v1/admin/conversations/user/{username}` 🔒 *Requer ADMIN ou MODERATOR*

## 📦 Estrutura do Projeto

```
src/main/java/com/caio/ollama_integration/
├── config/
│   ├── OpenApiConfig.java         # Configuração Swagger/OpenAPI
│   └── WebSecurityConfig.java     # Configuração Spring Security
├── controller/
│   ├── AdminController.java       # Endpoints de administração
│   ├── AuthController.java        # Endpoints de autenticação JWT
│   └── ConversationController.java # Endpoints de chat e histórico
├── dto/
│   ├── AuthRequest.java           # DTO de login (username/password)
│   ├── AuthResponse.java          # DTO de resposta JWT
│   ├── ConversationRequest.java   # DTO de requisição conversação
│   ├── ConversationResponse.java  # DTO de resposta conversação
│   ├── ConversationChatResponse.java # DTO de resposta chat
│   ├── CreateUserRequest.java     # DTO para criar usuário
│   ├── UserResponse.java          # DTO de resposta usuário
│   └── UpdateRolesRequest.java    # DTO para atualizar roles
├── model/
│   ├── Conversation.java          # Entidade conversação (MongoDB)
│   ├── Message.java               # Entidade mensagem
│   ├── User.java                  # Entidade usuário (MongoDB)
│   └── Role.java                  # Enum de roles
├── repository/
│   ├── ConversationRepository.java # Repository MongoDB
│   └── UserRepository.java        # Repository de usuários
├── security/
│   ├── JwtAuthenticationFilter.java  # Filtro de autenticação JWT
│   ├── RequiresRole.java          # Anotação para controle de roles
│   └── RoleCheckInterceptor.java  # Interceptor de verificação de roles
│   └── JwtUtil.java               # Utilitário JWT (geração/validação)
├── service/
│   ├── AuthService.java           # Lógica de autenticação
│   ├── ConversationService.java   # Lógica de histórico
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

### MongoDB não está acessível
- Verifique se o container está rodando: `docker ps | findstr mongodb`
- Verifique os logs: `docker logs mongodb`
- Teste a conexão: `docker exec -it mongodb mongosh -u admin -p admin123`

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
- [x] Implementar histórico de conversação com banco de dados
- [x] Sistema completo de roles e permissões (USER, MODERATOR, ADMIN)
- [ ] Adicionar suporte a embeddings
- [ ] Implementar RAG (Retrieval-Augmented Generation)
- [ ] Sistema de roles e permissões granular
- [ ] Rate limiting por usuário
- [ ] Métricas e monitoramento (Actuator)
- [ ] Rotação automática de access keys
