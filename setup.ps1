# Script para setup inicial do projeto
Write-Host "🚀 Iniciando setup do Ollama Integration..." -ForegroundColor Green

# 1. Verificar Docker
Write-Host "`n📦 Verificando Docker..." -ForegroundColor Cyan
try {
    docker --version
    docker-compose --version
} catch {
    Write-Host "❌ Docker não encontrado. Por favor, instale o Docker Desktop." -ForegroundColor Red
    exit 1
}

# 2. Subir containers
Write-Host "`n🐳 Subindo containers do Ollama..." -ForegroundColor Cyan
docker-compose up -d

# 3. Aguardar Ollama ficar pronto
Write-Host "`n⏳ Aguardando Ollama inicializar..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

# 4. Baixar modelo padrão
Write-Host "`n📥 Baixando modelo llama3.2 (isso pode demorar alguns minutos)..." -ForegroundColor Cyan
docker exec -it ollama ollama pull llama3.2

# 5. Verificar modelos instalados
Write-Host "`n✅ Modelos instalados:" -ForegroundColor Green
docker exec -it ollama ollama list

# 6. Compilar projeto
Write-Host "`n🔨 Compilando projeto Spring Boot..." -ForegroundColor Cyan
mvn clean install -DskipTests

Write-Host "`n✅ Setup concluído!" -ForegroundColor Green
Write-Host "`nPróximos passos:" -ForegroundColor Yellow
Write-Host "1. Execute: mvn spring-boot:run"
Write-Host "2. Acesse: http://localhost:8080/api/chat/health"
Write-Host "3. Interface Web do Ollama: http://localhost:3000"
