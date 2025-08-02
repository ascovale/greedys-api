# Script PowerShell per configurare Google Maps API Key
# Configurazione semplificata con variabili d'ambiente

param(
    [Parameter(Mandatory=$true, HelpMessage="Inserisci la tua Google Maps API Key")]
    [string]$GoogleMapsApiKey
)

Write-Host "🗺️ Configurazione Google Maps API Key" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green

# Imposta la variabile d'ambiente per l'utente corrente
try {
    [Environment]::SetEnvironmentVariable("GOOGLE_MAPS_API_KEY", $GoogleMapsApiKey, "User")
    Write-Host "✅ Google Maps API Key configurata come variabile d'ambiente utente" -ForegroundColor Green
    Write-Host "   Variabile: GOOGLE_MAPS_API_KEY" -ForegroundColor White
    Write-Host "   Scope: User (persistente tra riavvii)" -ForegroundColor White
    
    # Imposta anche per la sessione corrente
    $env:GOOGLE_MAPS_API_KEY = $GoogleMapsApiKey
    Write-Host "✅ Variabile impostata anche per la sessione corrente" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Errore nell'impostazione della variabile d'ambiente" -ForegroundColor Red
    Write-Host "   Errore: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Verifica della configurazione
Write-Host "`n🔍 Verifica configurazione..." -ForegroundColor Cyan

# Test della variabile d'ambiente
$testKey = [Environment]::GetEnvironmentVariable("GOOGLE_MAPS_API_KEY", "User")
if ($testKey -eq $GoogleMapsApiKey) {
    Write-Host "✅ Variabile d'ambiente configurata correttamente" -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: Variabile d'ambiente non corrisponde" -ForegroundColor Yellow
}

# Test veloce API Google
Write-Host "`n🧪 Test veloce Google Maps API..." -ForegroundColor Cyan
$testAddress = "Milano, Italia"
$testUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + [System.Web.HttpUtility]::UrlEncode($testAddress) + "&key=$GoogleMapsApiKey"

try {
    $response = Invoke-RestMethod -Uri $testUrl -Method GET -TimeoutSec 10
    
    if ($response.status -eq "OK") {
        Write-Host "✅ Google Maps API funziona correttamente!" -ForegroundColor Green
        Write-Host "   Test indirizzo: $testAddress" -ForegroundColor White
        Write-Host "   Risultati trovati: $($response.results.Count)" -ForegroundColor White
    } elseif ($response.status -eq "REQUEST_DENIED") {
        Write-Host "❌ API Key non valida o API non abilitata" -ForegroundColor Red
        Write-Host "   Verifica che la Geocoding API sia abilitata in Google Cloud Console" -ForegroundColor Yellow
    } else {
        Write-Host "⚠️  API risponde con errore: $($response.status)" -ForegroundColor Yellow
        if ($response.error_message) {
            Write-Host "   Messaggio: $($response.error_message)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "⚠️  Impossibile testare l'API (probabile problema di rete)" -ForegroundColor Yellow
    Write-Host "   Errore: $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host "`n📋 Riepilogo Configurazione:" -ForegroundColor Green
Write-Host "============================" -ForegroundColor Green
Write-Host "🔑 Google Maps API Key: ✅ Configurata" -ForegroundColor Green
Write-Host "📂 Variabile d'ambiente: GOOGLE_MAPS_API_KEY" -ForegroundColor White
Write-Host "🎯 Scope: User (persistente)" -ForegroundColor White

Write-Host "`n🚀 Prossimi passi:" -ForegroundColor Cyan
Write-Host "1. Riavvia VS Code o il terminale per applicare le variabili d'ambiente" -ForegroundColor White
Write-Host "2. Avvia l'applicazione: ./mvnw spring-boot:run" -ForegroundColor White
Write-Host "3. Testa il geocoding con: .\test-geocoding-simple.ps1" -ForegroundColor White

Write-Host "`n💡 Suggerimenti:" -ForegroundColor Yellow
Write-Host "- Per vedere la variabile: echo `$env:GOOGLE_MAPS_API_KEY" -ForegroundColor White
Write-Host "- Per rimuoverla: [Environment]::SetEnvironmentVariable('GOOGLE_MAPS_API_KEY', `$null, 'User')" -ForegroundColor White
Write-Host "- Configura restrizioni IP in Google Cloud Console per sicurezza" -ForegroundColor White
