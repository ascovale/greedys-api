# Script semplice per configurare Google Maps API Key
# Uso: .\setup-google-key.ps1 "la_tua_api_key_qui"

param(
    [Parameter(Mandatory=$true, HelpMessage="Inserisci la tua Google Maps API Key")]
    [string]$ApiKey
)

Write-Host "🔑 Configurazione Google Maps API Key..." -ForegroundColor Green

# Imposta la variabile d'ambiente per l'utente corrente
try {
    [Environment]::SetEnvironmentVariable("GOOGLE_MAPS_API_KEY", $ApiKey, "User")
    Write-Host "✅ Variabile d'ambiente GOOGLE_MAPS_API_KEY configurata" -ForegroundColor Green
    
    # Aggiorna anche il file .env
    $envContent = @"
# Google Maps API Key configurata automaticamente
GOOGLE_MAPS_API_KEY=$ApiKey

# Google OAuth (opzionale)
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=

# Application base URL
APP_BASE_URL=http://localhost:8080
"@
    
    $envContent | Out-File -FilePath ".env" -Encoding UTF8
    Write-Host "✅ File .env aggiornato" -ForegroundColor Green
    
    Write-Host "`n🎉 Configurazione completata!" -ForegroundColor Green
    Write-Host "📝 Prossimi passi:" -ForegroundColor Yellow
    Write-Host "   1. Riavvia PowerShell per caricare la nuova variabile d'ambiente" -ForegroundColor White
    Write-Host "   2. Avvia l'applicazione: ./mvnw spring-boot:run" -ForegroundColor White
    Write-Host "   3. Testa il geocoding: .\test-geocoding-simple.ps1" -ForegroundColor White
    
} catch {
    Write-Host "❌ Errore nella configurazione: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Prova a eseguire PowerShell come Amministratore" -ForegroundColor Yellow
}

Write-Host "`n💡 Nota: Il servizio funziona anche senza API key usando OpenStreetMap come fallback" -ForegroundColor Cyan
