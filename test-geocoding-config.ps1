# Script di Test per verificare la configurazione del Geocoding Service
# Esegui questo script per testare che le API key siano configurate correttamente

param(
    [Parameter(HelpMessage="Indirizzo da testare")]
    [string]$TestAddress = "Via Roma 1, Milano, Italia"
)

Write-Host "🧪 Test di Configurazione Geocoding Service" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

# Verifica che l'applicazione sia in esecuzione
Write-Host "🔍 Verifica che l'applicazione sia in esecuzione..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 5
    if ($response.status -eq "UP") {
        Write-Host "✅ Applicazione in esecuzione" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Applicazione avviata ma stato: $($response.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Applicazione non raggiungibile su http://localhost:8080" -ForegroundColor Red
    Write-Host "   Verifica che l'applicazione sia avviata" -ForegroundColor Yellow
    Write-Host "   Comando: ./mvnw spring-boot:run" -ForegroundColor White
    
    # Prova con Docker
    try {
        $dockerResponse = Invoke-RestMethod -Uri "https://localhost:5050/actuator/health" -Method GET -TimeoutSec 5 -SkipCertificateCheck
        Write-Host "✅ Applicazione Docker in esecuzione su porta 5050" -ForegroundColor Green
    } catch {
        Write-Host "❌ Applicazione Docker non raggiungibile" -ForegroundColor Red
        exit 1
    }
}

# Verifica dei logs per configurazione geocoding
Write-Host "`n📋 Verifica configurazione geocoding nei logs..." -ForegroundColor Cyan

# Se è Docker
$dockerLogs = docker-compose logs spring-app 2>$null | Select-String -Pattern "geocoding|google|nominatim" -CaseSensitive:$false
if ($dockerLogs) {
    Write-Host "🐳 Logs Docker relativi al geocoding:" -ForegroundColor Blue
    $dockerLogs | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
}

# Test della configurazione Google Maps API
Write-Host "`n🗺️  Test Google Maps API..." -ForegroundColor Cyan

$testUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + [System.Web.HttpUtility]::UrlEncode($TestAddress)

# Leggi la API key dalle variabili d'ambiente
$apiKey = $env:GOOGLE_MAPS_API_KEY
if (-not $apiKey) {
    # Prova a leggere da file secret Docker
    if (Test-Path "/run/secrets/google_maps_api_key") {
        $apiKey = Get-Content "/run/secrets/google_maps_api_key" -Raw
    }
}

if ($apiKey -and $apiKey.Trim() -ne "") {
    $testUrl += "&key=$apiKey"
    
    try {
        $googleResponse = Invoke-RestMethod -Uri $testUrl -Method GET -TimeoutSec 10
        
        if ($googleResponse.status -eq "OK") {
            Write-Host "✅ Google Maps API funziona correttamente" -ForegroundColor Green
            Write-Host "   Indirizzo testato: $TestAddress" -ForegroundColor White
            Write-Host "   Risultati trovati: $($googleResponse.results.Count)" -ForegroundColor White
            
            if ($googleResponse.results.Count -gt 0) {
                $result = $googleResponse.results[0]
                Write-Host "   Indirizzo formattato: $($result.formatted_address)" -ForegroundColor White
                Write-Host "   Coordinate: $($result.geometry.location.lat), $($result.geometry.location.lng)" -ForegroundColor White
            }
        } else {
            Write-Host "⚠️  Google Maps API risponde ma con errore: $($googleResponse.status)" -ForegroundColor Yellow
            Write-Host "   Messaggio: $($googleResponse.error_message)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Errore nella chiamata a Google Maps API" -ForegroundColor Red
        Write-Host "   Errore: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠️  Google Maps API Key non configurata" -ForegroundColor Yellow
    Write-Host "   Verrà usato Nominatim come fallback" -ForegroundColor White
}

# Test Nominatim (fallback)
Write-Host "`n🌍 Test Nominatim (OpenStreetMap)..." -ForegroundColor Cyan

$nominatimUrl = "https://nominatim.openstreetmap.org/search?q=" + [System.Web.HttpUtility]::UrlEncode($TestAddress) + "&format=json&limit=1&addressdetails=1"

try {
    $headers = @{
        'User-Agent' = 'GreedysAPI/1.0'
    }
    
    $nominatimResponse = Invoke-RestMethod -Uri $nominatimUrl -Method GET -Headers $headers -TimeoutSec 10
    
    if ($nominatimResponse -and $nominatimResponse.Count -gt 0) {
        Write-Host "✅ Nominatim funziona correttamente" -ForegroundColor Green
        $result = $nominatimResponse[0]
        Write-Host "   Indirizzo testato: $TestAddress" -ForegroundColor White
        Write-Host "   Nome visualizzato: $($result.display_name)" -ForegroundColor White
        Write-Host "   Coordinate: $($result.lat), $($result.lon)" -ForegroundColor White
    } else {
        Write-Host "⚠️  Nominatim non ha trovato risultati per l'indirizzo" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Errore nella chiamata a Nominatim" -ForegroundColor Red
    Write-Host "   Errore: $($_.Exception.Message)" -ForegroundColor Red
}

# Riepilogo
Write-Host "`n📊 Riepilogo Configurazione:" -ForegroundColor Green
Write-Host "============================" -ForegroundColor Green

if ($apiKey -and $apiKey.Trim() -ne "") {
    Write-Host "🗺️  Google Maps API: ✅ Configurata" -ForegroundColor Green
} else {
    Write-Host "🗺️  Google Maps API: ❌ Non configurata" -ForegroundColor Red
}

Write-Host "🌍 Nominatim (Fallback): ✅ Disponibile" -ForegroundColor Green

Write-Host "`n💡 Suggerimenti:" -ForegroundColor Cyan
if (-not $apiKey -or $apiKey.Trim() -eq "") {
    Write-Host "   - Configura la Google Maps API Key per migliori risultati" -ForegroundColor Yellow
    Write-Host "   - Guida: .\GEOCODING_SETUP.md" -ForegroundColor White
}
Write-Host "   - Controlla i logs per eventuali errori: docker-compose logs spring-app" -ForegroundColor White
Write-Host "   - Monitor quotas Google Cloud Console se usi Google Maps API" -ForegroundColor White
