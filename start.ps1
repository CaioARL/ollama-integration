# Ollama Integration Stack - Start Script
# PowerShell version

Write-Host "🚀 Iniciando Ollama Integration Stack..." -ForegroundColor Green

# Verificar se Docker está rodando
try {
    docker info | Out-Null
} catch {
    Write-Host "❌ Docker não está rodando. Inicie o Docker primeiro." -ForegroundColor Red
    exit 1
}

# Parar containers existentes
Write-Host "🛑 Parando containers existentes..." -ForegroundColor Yellow
docker-compose down

# Build da aplicação
Write-Host "🔨 Building aplicação..." -ForegroundColor Cyan
docker-compose build --no-cache ollama-integration-app

# Iniciar infraestrutura primeiro
Write-Host "📦 Iniciando infraestrutura (MongoDB, Kafka, Zookeeper, Ollama)..." -ForegroundColor Cyan
docker-compose up -d mongodb zookeeper kafka ollama

# Aguardar infraestrutura estar pronta
Write-Host "⏳ Aguardando infraestrutura ficar pronta (30s)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Verificar health
Write-Host "🏥 Verificando health da infraestrutura..." -ForegroundColor Cyan
docker-compose ps

# Iniciar aplicação
Write-Host "🚀 Iniciando aplicação..." -ForegroundColor Green
docker-compose up -d ollama-integration-app

# Aguardar aplicação iniciar
Write-Host "⏳ Aguardando aplicação iniciar (20s)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

# Status final
Write-Host ""
Write-Host "✅ Stack iniciada com sucesso!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Status dos containers:" -ForegroundColor Cyan
docker-compose ps
Write-Host ""
Write-Host "🌐 Endpoints disponíveis:" -ForegroundColor Cyan
Write-Host "  - API: http://localhost:8080" -ForegroundColor White
Write-Host "  - Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "  - Health: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "📝 Para ver logs: docker-compose logs -f ollama-integration-app" -ForegroundColor Yellow
Write-Host "🛑 Para parar: docker-compose down" -ForegroundColor Yellow
