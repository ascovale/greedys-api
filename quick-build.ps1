# Script per build rapido con cache Docker
Write-Host "🐳 Avvio build Docker ottimizzato..." -ForegroundColor Green

# Build con cache
docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest . --cache-from registry.gitlab.com/psychoorange/greedys_api:latest

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build completato con successo!" -ForegroundColor Green
    
    # Opzioni aggiuntive
    $choice = Read-Host "Vuoi pushare l'immagine? (y/n)"
    if ($choice -eq "y" -or $choice -eq "Y") {
        Write-Host "📤 Push dell'immagine..." -ForegroundColor Yellow
        docker push registry.gitlab.com/psychoorange/greedys_api:latest
    }
    
    $choice = Read-Host "Vuoi avviare il container localmente? (y/n)"
    if ($choice -eq "y" -or $choice -eq "Y") {
        Write-Host "🚀 Avvio container..." -ForegroundColor Yellow
        docker-compose up -d
    }
} else {
    Write-Host "❌ Build fallito!" -ForegroundColor Red
}
