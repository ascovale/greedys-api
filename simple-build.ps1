# Script semplificato per build rapido
Write-Host "🐳 Build Docker in corso..." -ForegroundColor Green

docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest . --cache-from registry.gitlab.com/psychoorange/greedys_api:latest

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build completato!" -ForegroundColor Green
} else {
    Write-Host "❌ Build fallito!" -ForegroundColor Red
}
