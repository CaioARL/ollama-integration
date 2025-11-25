# Ollama Integration Backend

Backend Spring Boot para integração com Ollama (LLM local) usando Spring AI, com autenticação JWT, processamento distribuído com Kafka e documentação Swagger/OpenAPI.

## 📋 Pré-requisitos

- **Opção 1 (Docker - Recomendado)**: Docker e Docker Compose
- **Opção 2 (Local)**: Java 21+, Maven 3.6+, MongoDB, Kafka, Ollama

## 🚀 Início Rápido (Docker)

### Método 1: Script Automático

**Windows (PowerShell):**
```powershell
.\start.ps1
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

### Método 2: Manual

```bash
# Build e iniciar toda a stack
docker-compose up -d --build

# Ver logs
docker-compose logs -f ollama-integration-app
```

Isso irá iniciar:
- 🤖 **Ollama** (porta 11434) - LLM local
- 🍃 **MongoDB** (porta 27017) - Banco de dados
- 📨 **Kafka** (porta 9092) - Fila de mensagens distribuída
- 🔧 **Zookeeper** (porta 2181) - Coordenação Kafka
- 🚀 **Aplicação** (porta 8080) - API REST

### 2. Baixar modelos

Acesse o container do Ollama e baixe os modelos necessários:

**Modelo LLM para chat:**
```bash
docker exec -it ollama ollama pull llama3.2
```

**Modelo de embeddings para busca semântica:**
```bash
docker exec -it ollama ollama pull all-minilm
```

Outros modelos disponíveis:
- **LLM (Chat):**
  - `llama3.2` (recomendado, ~2GB)
  - `llama3.2:1b` (menor, ~1.3GB)
  - `mistral` (~4GB)
  - `codellama` (especializado em código)

- **Embeddings (Busca Semântica):**
  - `all-minilm` (rápido, ~25MB)
  - `nomic-embed-text` (melhor qualidade, ~274MB)
  - `mxbai-embed-large` (alta qualidade, ~670MB)

Para listar modelos instalados:
```bash
docker exec -it ollama ollama list
```

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                      Docker Compose Stack                    │
├──────────────┬────────────────┬────────────┬────────────────┤
│   Aplicação  │     Kafka      │  MongoDB   │    Ollama      │
│   (Spring)   │  (Mensageria)  │   (NoSQL)  │    (LLM)       │
│   :8080      │     :9092      │   :27017   │    :11434      │
└──────────────┴────────────────┴────────────┴────────────────┘
       ↓                ↓              ↓             ↓
   REST API      Fila Embeddings   Histórico    Modelos IA
```

### Fluxo de Processamento

1. **Request** → API REST recebe conversação
2. **Processing** → Gera resposta com Ollama
3. **Queue** → Enfileira embedding no Kafka
4. **Consumer** → Worker processa embedding assincronamente
5. **Storage** → Salva no MongoDB para busca semântica

## 📚 Documentação

- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Docker Guide**: [DOCKER-GUIDE.md](DOCKER-GUIDE.md)
- **Kafka Embeddings**: [KAFKA-EMBEDDINGS.md](KAFKA-EMBEDDINGS.md)

## 💻 Desenvolvimento Local

Para desenvolver localmente com hot-reload:

```bash
# 1. Subir apenas infraestrutura
docker-compose -f docker-compose.dev.yml up -d

# 2. Rodar aplicação em modo dev
mvn spring-boot:run

# Ou via IDE (IntelliJ/Eclipse/VS Code)
```

Vantagens:
- ✅ Hot reload automático
- ✅ Debug facilitado
- ✅ Logs diretos no terminal
- ✅ Infraestrutura isolada

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
OLLAMA_EMBEDDING_MODEL=all-minilm

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

### Buscar Conversações Semanticamente

**GET** `/v1/conversations/search?query={texto}&limit={numero}` 🔒 *Requer autenticação*

Busca conversações anteriores semanticamente similares à query usando embeddings vetoriais.

**Parâmetros:**
- `query`: Texto da busca (ex: "conversas sobre inteligência artificial")
- `limit`: Número máximo de resultados (padrão: 5)

## 🤖 RAG (Retrieval-Augmented Generation) Automático

O sistema implementa **RAG automaticamente** em todas as conversações! Quando você faz uma pergunta:

1. **🔍 Busca Inteligente**: O sistema busca automaticamente conversações e informações anteriores relevantes
2. **📚 Contexto Enriquecido**: Adiciona o contexto encontrado ao prompt da IA (invisível para você)
3. **💡 Respostas Melhores**: A IA responde baseada no histórico e conhecimento acumulado
4. **📝 Auto-Indexação**: Cada conversação é automaticamente indexada para buscas futuras

**Exemplo Prático:**

```bash
# 1ª Conversa - você ensina algo à IA
POST /v1/conversations/abc123/chat
{
  "message": "Spring Boot usa anotações como @RestController para criar APIs REST"
}

# 2ª Conversa - semanas depois, em outra conversação
POST /v1/conversations/xyz789/chat
{
  "message": "Como criar uma API REST em Java?"
}

# A IA automaticamente:
# - Busca a conversa anterior sobre Spring Boot
# - Usa como contexto
# - Responde: "Você pode usar Spring Boot com @RestController..."
```

**Benefícios:**
- ✅ **Memória de longo prazo**: A IA "lembra" de conversas anteriores
- ✅ **Zero configuração**: Funciona automaticamente, sem ação do usuário
- ✅ **Aprendizado contínuo**: Quanto mais você usa, mais inteligente fica
- ✅ **Privacidade**: Cada usuário tem sua própria base de conhecimento

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
- Para embeddings: `docker exec -it ollama ollama pull all-minilm`

### Erro ao gerar embeddings
- Certifique-se de que o modelo de embeddings está instalado
- Verifique a configuração `OLLAMA_EMBEDDING_MODEL` no `.env`
- Teste: `docker exec -it ollama ollama run all-minilm "test"`

### Porta já em uso
- Altere a porta em `docker-compose.yml` ou `application.properties`

## 🔍 Monitoramento Kafka

```bash
# Listar tópicos
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Ver mensagens em tempo real
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic embedding-requests \
  --from-beginning

# Ver consumer groups
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list

# Ver lag do consumer
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group embedding-service-group \
  --describe
```

## 📚 Recursos

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Swagger/OpenAPI Specification](https://swagger.io/specification/)
- [Ollama Models](https://ollama.ai/library)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

## 🧠 Como Funciona o RAG Automático

O sistema usa **embeddings vetoriais** para implementar RAG de forma transparente:

### Fluxo Automático

```
1. Você envia mensagem → "Como usar Spring Boot?"

2. Sistema busca contexto relevante:
   ┌─────────────────────────────────┐
   │ Embedding da sua pergunta       │
   │ ↓                               │
   │ Busca em conversas anteriores   │
   │ ↓                               │
   │ Top 3 documentos similares      │
   └─────────────────────────────────┘

3. Monta prompt enriquecido:
   ┌──────────────────────────────────────────┐
   │ Contexto: [conversas anteriores]        │
   │ Pergunta: Como usar Spring Boot?        │
   └──────────────────────────────────────────┘

4. IA responde com contexto ← Você só vê isso

5. Sistema auto-indexa a conversa para futuras buscas
```

### Tecnologia

- **Embedding Model**: `all-minilm` (Ollama)
- **Busca Semântica**: Similaridade cosseno
- **Auto-Indexação**: Toda conversa vira documento pesquisável
- **Privacidade**: Dados isolados por usuário

## 🎯 Próximos Passos

- [x] Adicionar autenticação JWT
- [x] Documentação Swagger/OpenAPI
- [x] Configuração com variáveis de ambiente (.env)
- [x] Autenticação baseada em subject/accessKey
- [x] Adicionar streaming de respostas (SSE)
- [x] Implementar histórico de conversação com banco de dados
- [x] Sistema completo de roles e permissões (USER, MODERATOR, ADMIN)
- [x] Adicionar suporte a embeddings e busca semântica
- [x] Implementar RAG (Retrieval-Augmented Generation) automático
- [x] Indexação automática de conversações como documentos
- [ ] Upload e processamento de arquivos PDF/TXT para base de conhecimento
- [ ] Ajuste de relevância RAG (threshold de similaridade configurável)
- [ ] Rate limiting por usuário
- [ ] Métricas e monitoramento (Actuator)
- [ ] MongoDB Atlas Vector Search (para produção em escala)
