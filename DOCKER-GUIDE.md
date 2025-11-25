# Docker Build & Deploy Guide

## 🐳 Build Local

```bash
# Build da imagem
docker build -t ollama-integration:latest .

# Executar apenas a aplicação (assumindo infra já rodando)
docker run -d \
  --name ollama-app \
  --network ollama-integration_ollama-network \
  -p 8080:8080 \
  -e OLLAMA_BASE_URL=http://ollama:11434 \
  -e MONGODB_URI=mongodb://admin:admin123@mongodb:27017/ollama_db?authSource=admin \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  ollama-integration:latest
```

## 🚀 Docker Compose - Stack Completa

```bash
# Build e start de todos os serviços
docker-compose up -d --build

# Ver logs
docker-compose logs -f ollama-integration-app

# Parar tudo
docker-compose down

# Parar e remover volumes (limpar dados)
docker-compose down -v
```

## 📊 Verificar Status

```bash
# Status dos containers
docker-compose ps

# Health check individual
docker inspect ollama-integration-app --format='{{.State.Health.Status}}'

# Logs específicos
docker-compose logs ollama-integration-app
docker-compose logs kafka
docker-compose logs mongodb
```

## 🔧 Desenvolvimento

### Build apenas do app sem subir

```bash
# Build da imagem
docker-compose build ollama-integration-app

# Rebuild forçado (sem cache)
docker-compose build --no-cache ollama-integration-app
```

### Escalar Horizontalmente

```bash
# Rodar 3 instâncias da aplicação
docker-compose up -d --scale ollama-integration-app=3

# Kafka vai distribuir automaticamente as partições!
```

### Executar comandos no container

```bash
# Shell no container
docker exec -it ollama-integration-app sh

# Ver variáveis de ambiente
docker exec ollama-integration-app env

# Ver logs do Java
docker exec ollama-integration-app cat /app/logs/application.log
```

## 🐛 Troubleshooting

### App não conecta no Kafka
```bash
# Verificar conectividade
docker exec ollama-integration-app ping kafka

# Ver logs do Kafka
docker-compose logs kafka | grep ERROR
```

### App não conecta no MongoDB
```bash
# Testar conexão
docker exec ollama-integration-app wget --spider mongodb:27017

# Acessar MongoDB shell
docker exec -it mongodb mongosh -u admin -p admin123
```

### Rebuild após mudanças no código
```bash
# Parar app, rebuild e iniciar
docker-compose stop ollama-integration-app
docker-compose build ollama-integration-app
docker-compose up -d ollama-integration-app
```

## 📦 Imagem Multi-stage

O Dockerfile usa **multi-stage build**:
- **Stage 1 (builder)**: Compila com Maven (JDK 21)
- **Stage 2 (final)**: Runtime leve com JRE 21 Alpine

**Tamanho**: ~250-300MB (vs ~600MB com JDK completo)

## 🔐 Segurança

- ✅ Usuário não-root (`spring:spring`)
- ✅ JRE em vez de JDK (menor superfície de ataque)
- ✅ Health checks configurados
- ✅ Logs estruturados
- ⚠️ **IMPORTANTE**: Mudar JWT_SECRET em produção!

## 🌐 Endpoints

Após iniciar:

- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/actuator/health
- **API Docs**: http://localhost:8080/api-docs

## 📈 Recursos Recomendados

### Desenvolvimento
```yaml
JAVA_OPTS: "-Xms256m -Xmx512m"
```

### Produção
```yaml
JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 🎯 Próximos Passos

- [ ] Docker Registry (push para DockerHub/ECR)
- [ ] Kubernetes manifests (deployment.yaml, service.yaml)
- [ ] Nginx reverse proxy
- [ ] Prometheus + Grafana para métricas
