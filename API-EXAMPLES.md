# API Examples - Ollama Integration

Coleção de exemplos de uso da API via curl.

## 🔐 Autenticação

### Registrar Novo Usuário

```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "Dev@123456",
    "email": "dev@example.com"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "developer",
  "expiresIn": 86400000
}
```

### Login

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "Dev@123456"
  }'
```

## 💬 Conversações

### Criar Nova Conversação

```bash
curl -X POST http://localhost:8080/v1/conversations \
  -H "Authorization: Bearer {SEU_TOKEN}" \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "id": "60d5ec49f1b2c8a3d4e5f6a7",
  "username": "developer",
  "title": "Novo CHAT",
  "messages": [],
  "createdAt": "2025-11-25T14:30:00",
  "model": null
}
```

### Enviar Mensagem (Resposta Normal)

```bash
curl -X POST http://localhost:8080/v1/conversations/60d5ec49f1b2c8a3d4e5f6a7/messages \
  -H "Authorization: Bearer {SEU_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Como criar uma API REST com Spring Boot?",
    "model": "llama3.2"
  }'
```

### Enviar Mensagem (Streaming)

```bash
curl -X POST http://localhost:8080/v1/conversations/60d5ec49f1b2c8a3d4e5f6a7/messages/stream \
  -H "Authorization: Bearer {SEU_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Explique Kafka em detalhes",
    "model": "llama3.2"
  }'
```

**Response (Server-Sent Events):**
```
data: Para

data: criar

data: uma

data: API

data: REST

data: com

data: Spring

data: Boot...
```

### Listar Conversações

```bash
curl -X GET http://localhost:8080/v1/conversations \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

### Buscar Conversações por Texto

```bash
curl -X GET "http://localhost:8080/v1/conversations/search?query=spring%20boot&limit=5" \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

### Obter Conversação Específica

```bash
curl -X GET http://localhost:8080/v1/conversations/60d5ec49f1b2c8a3d4e5f6a7 \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

### Atualizar Título da Conversação

```bash
curl -X PATCH http://localhost:8080/v1/conversations/60d5ec49f1b2c8a3d4e5f6a7 \
  -H "Authorization: Bearer {SEU_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot APIs"
  }'
```

### Deletar Conversação

```bash
curl -X DELETE http://localhost:8080/v1/conversations/60d5ec49f1b2c8a3d4e5f6a7 \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

## 📊 Embeddings e Busca Semântica

### Criar Documento com Embedding

```bash
curl -X POST http://localhost:8080/v1/embeddings/documents \
  -H "Authorization: Bearer {SEU_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Boot facilita a criação de aplicações standalone...",
    "documentType": "tutorial",
    "metadata": {
      "category": "backend",
      "tags": ["spring", "java", "api"],
      "author": "developer"
    }
  }'
```

**Nota:** O processamento ocorre via Kafka assincronamente!

### Buscar Documentos Similares

```bash
curl -X GET "http://localhost:8080/v1/embeddings/search?query=spring%20boot%20rest%20api&limit=3" \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

**Response:**
```json
[
  {
    "id": "60d5ec49f1b2c8a3d4e5f6a8",
    "content": "Spring Boot facilita a criação...",
    "documentType": "tutorial",
    "metadata": {
      "category": "backend",
      "_similarity_score": 0.89
    },
    "createdAt": "2025-11-25T14:30:00"
  }
]
```

### Listar Documentos do Usuário

```bash
curl -X GET http://localhost:8080/v1/embeddings/documents \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

### Deletar Documento

```bash
curl -X DELETE http://localhost:8080/v1/embeddings/documents/60d5ec49f1b2c8a3d4e5f6a8 \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

## 🤖 Modelos

### Listar Modelos Disponíveis

```bash
curl -X GET http://localhost:8080/v1/models \
  -H "Authorization: Bearer {SEU_TOKEN}"
```

**Response:**
```json
{
  "models": [
    {
      "name": "llama3.2:latest",
      "size": 2065848320,
      "digest": "a80c4f17acd55265feec403c7aef86be0c25983ab279d83f3bcd3abbcb5b8b72",
      "modified_at": "2025-11-25T10:15:30.123Z"
    },
    {
      "name": "all-minilm:latest",
      "size": 45931008,
      "digest": "0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3d3ae043935e93b8a47",
      "modified_at": "2025-11-25T10:16:45.789Z"
    }
  ]
}
```

## 🏥 Health & Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "mongo": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Actuator Endpoints

```bash
# Info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics

# Environment
curl http://localhost:8080/actuator/env
```

## 🔄 Workflow Completo

```bash
# 1. Registrar
TOKEN=$(curl -s -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"dev","password":"Dev@123","email":"dev@test.com"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 2. Criar conversação
CONV_ID=$(curl -s -X POST http://localhost:8080/v1/conversations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq -r '.id')

echo "Conversation ID: $CONV_ID"

# 3. Enviar mensagem
curl -X POST http://localhost:8080/v1/conversations/$CONV_ID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Olá! Como você está?",
    "model": "llama3.2"
  }'

# 4. Ver histórico
curl -X GET http://localhost:8080/v1/conversations/$CONV_ID \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.messages'
```

## 📝 Notas

- **Token JWT**: Válido por 24h (86400000ms)
- **Embeddings**: Processados assincronamente via Kafka
- **Streaming**: Use `/messages/stream` para respostas em tempo real
- **Rate Limiting**: Não implementado (TODO)
- **Paginação**: Não implementado (TODO)

## 🐛 Troubleshooting

### Token Expirado
```json
{
  "timestamp": "2025-11-25T14:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token expirado"
}
```
**Solução**: Fazer login novamente

### Modelo não encontrado
```json
{
  "error": "Model not found: mistral"
}
```
**Solução**: 
```bash
docker exec -it ollama ollama pull mistral
```

### Kafka não conectado
```json
{
  "error": "Failed to send message to Kafka"
}
```
**Solução**: Verificar se Kafka está rodando:
```bash
docker-compose ps kafka
docker logs kafka
```
