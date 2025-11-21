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

Acesse a documentação interativa da API em: `http://localhost:8080/swagger-ui.html`

## ⚙️ Configuração com .env

O projeto utiliza variáveis de ambiente através do arquivo `.env`. 

### 1. Criar arquivo .env

Copie o arquivo de exemplo e configure suas credenciais:

```bash
cp .env.example .env
```

Edite o `.env` com suas configurações:

```properties
# Credenciais de Autenticação
AUTH_SUBJECT=myapp
AUTH_ACCESS_KEY=secretkey123

# JWT Secret (altere para produção!)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Configurações do Ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
```

⚠️ **Importante**: O arquivo `.env` está no `.gitignore` e **não deve** ser commitado no repositório!

## 🔐 Autenticação JWT

### 1. Obter Token JWT

**POST** `/v1/api/auth/login`

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

**POST** `/v1/api/auth/validate`

```
Authorization: Bearer {seu_token}
```

**Response:**
```json
{
  "valid": true,
  "username": "admin",
  "message": "Token válido"
}
```

### 4. Informações do Usuário

**GET** `/v1/api/auth/user-info`

```
Authorization: Bearer {seu_token}
```

## 🔌 Endpoints da API

### Chat com o Agente de IA

**POST** `/v1/api/chat` 🔒 *Requer autenticação*

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

### Health Check

**GET** `/v1/api/chat/health` 🔒 *Requer autenticação*

Verifica se o Ollama está acessível e funcionando.

## 📝 Variáveis de Ambiente (.env)

Todas as configurações sensíveis são gerenciadas através do arquivo `.env`:

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| `AUTH_SUBJECT` | Subject para autenticação | `myapp` |
| `AUTH_ACCESS_KEY` | Chave de acesso secreta | `secretkey123` |
| `JWT_SECRET` | Secret para assinatura JWT | (gerado) |
| `JWT_EXPIRATION` | Tempo de expiração do token (ms) | `3600000` |
| `OLLAMA_BASE_URL` | URL do Ollama | `http://localhost:11434` |
| `OLLAMA_MODEL` | Modelo LLM padrão | `llama3.2` |
| `OLLAMA_TEMPERATURE` | Temperatura do modelo (0-1) | `0.7` |
| `SERVER_PORT` | Porta do servidor | `8080` |

## 🧪 Testando a API

### 1. Obter Token JWT (PowerShell):

```powershell
$loginBody = @{
    subject = "myapp"
    accessKey = "secretkey123"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8080/v1/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
$token = $authResponse.token
Write-Host "Token obtido: $token"
```

### 2. Usar o Chat com Token (PowerShell):

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$chatBody = @{
    message = "Explique o que é Spring Boot"
    model = "llama3.2"
} | ConvertTo-Json

$chatResponse = Invoke-RestMethod -Uri "http://localhost:8080/v1/api/chat" -Method POST -Body $chatBody -Headers $headers
Write-Host $chatResponse.response
```

### 3. Com cURL:

```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/v1/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"subject":"myapp","accessKey":"secretkey123"}' | jq -r '.token')

# Chat
curl -X POST http://localhost:8080/v1/api/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Explique o que é Spring Boot"}'
```

## 📦 Estrutura do Projeto

```
src/main/java/com/caio/ollama_integration/
├── config/
│   ├── OpenApiConfig.java         # Configuração Swagger/OpenAPI
│   └── SecurityConfig.java        # Configuração Spring Security
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
│   └── OllamaService.java         # Lógica de integração com Ollama
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
- Certifique-se de obter o token JWT primeiro através de `/v1/api/auth/login`
- Verifique se o header `Authorization: Bearer {token}` está correto
- Confirme se o token não expirou (validade de 1 hora)

### Ollama não está acessível
- Verifique se o container está rodando: `docker ps`
- Verifique os logs: `docker logs ollama`
- Teste a conexão: `curl http://localhost:11434/api/tags`

### Modelo não encontrado
- Liste os modelos instalados: `docker exec -it ollama ollama list`
- Baixe o modelo necessário: `docker exec -it ollama ollama pull llama3.2`

### Porta já em uso
- Altere a porta em `docker-compose.yml` ou `application.properties`

## 🔐 Segurança

⚠️ **Importante para Produção:**

1. **Proteja o .env**: Nunca commite o arquivo `.env` no repositório
2. **Altere as Credenciais**: Defina valores seguros para `AUTH_SUBJECT` e `AUTH_ACCESS_KEY`
3. **JWT Secret Forte**: Gere uma chave secreta criptograficamente segura
4. **Use HTTPS**: Configure SSL/TLS para comunicação segura
5. **Rotação de Chaves**: Implemente rotação periódica de access keys
6. **Rate Limiting**: Adicione limitação de taxa para evitar abuso
7. **Auditoria**: Implemente logs de auditoria para tentativas de login
8. **Variáveis de Ambiente**: Use secrets managers em produção (AWS Secrets Manager, Azure Key Vault, etc.)

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
- [ ] Adicionar streaming de respostas
- [ ] Implementar histórico de conversação com banco de dados
- [ ] Adicionar suporte a embeddings
- [ ] Implementar RAG (Retrieval-Augmented Generation)
- [ ] Sistema de roles e permissões granular
- [ ] Rate limiting por usuário
- [ ] Métricas e monitoramento (Actuator)
- [ ] Rotação automática de access keys
