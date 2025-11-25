#!/bin/bash
set -e

echo "🚀 Iniciando Ollama Integration Stack..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando. Inicie o Docker primeiro."
    exit 1
fi

# Parar containers existentes
echo "🛑 Parando containers existentes..."
docker-compose down

# Build da aplicação
echo "🔨 Building aplicação..."
docker-compose build --no-cache ollama-integration-app

# Iniciar infraestrutura primeiro
echo "📦 Iniciando infraestrutura (MongoDB, Kafka, Zookeeper, Ollama)..."
docker-compose up -d mongodb zookeeper kafka ollama

# Aguardar infraestrutura estar pronta
echo "⏳ Aguardando infraestrutura ficar pronta (30s)..."
sleep 30

# Verificar health
echo "🏥 Verificando health da infraestrutura..."
docker-compose ps

# Iniciar aplicação
echo "🚀 Iniciando aplicação..."
docker-compose up -d ollama-integration-app

# Aguardar aplicação iniciar
echo "⏳ Aguardando aplicação iniciar (20s)..."
sleep 20

# Status final
echo ""
echo "✅ Stack iniciada com sucesso!"
echo ""
echo "📊 Status dos containers:"
docker-compose ps
echo ""
echo "🌐 Endpoints disponíveis:"
echo "  - API: http://localhost:8080"
echo "  - Swagger: http://localhost:8080/swagger-ui.html"
echo "  - Health: http://localhost:8080/actuator/health"
echo ""
echo "📝 Para ver logs: docker-compose logs -f ollama-integration-app"
echo "🛑 Para parar: docker-compose down"
