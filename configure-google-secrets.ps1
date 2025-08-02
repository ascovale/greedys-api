# Script PowerShell per configurare i Docker Secrets per Greedys API
# Esegui questo script come Amministratore

param(
    [Parameter(Mandatory=$true, HelpMessage="Inserisci la tua Google Maps API Key")]
    [string]$GoogleMapsApiKey,
    
    [Parameter(HelpMessage="Inserisci il Google OAuth Client ID (opzionale)")]
    [string]$GoogleOAuthClientId = "",
    
    [Parameter(HelpMessage="Inserisci il Google OAuth Client Secret (opzionale)")]
    [string]$GoogleOAuthClientSecret = ""
)

Write-Host "🔐 Configurazione Docker Secrets per Greedys API" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green

# Verifica che Docker sia in esecuzione
try {
    docker info | Out-Null
    Write-Host "✅ Docker è in esecuzione" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker non è in esecuzione o non è installato" -ForegroundColor Red
    Write-Host "   Avvia Docker Desktop e riprova" -ForegroundColor Yellow
    exit 1
}

# Inizializza Docker Swarm se non è già inizializzato
$swarmStatus = docker info --format "{{.Swarm.LocalNodeState}}" 2>$null
if ($swarmStatus -ne "active") {
    Write-Host "🔄 Inizializzazione Docker Swarm..." -ForegroundColor Yellow
    docker swarm init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Errore nell'inizializzazione di Docker Swarm" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Docker Swarm inizializzato" -ForegroundColor Green
}

# Funzione per creare un secret
function Create-DockerSecret {
    param($secretName, $secretValue)
    
    if ($secretValue -and $secretValue.Trim() -ne "") {
        # Controlla se il secret esiste già
        $existingSecret = docker secret ls --filter "name=$secretName" --format "{{.Name}}" 2>$null
        if ($existingSecret -eq $secretName) {
            Write-Host "⚠️  Secret '$secretName' esiste già. Vuoi sostituirlo? (s/n): " -ForegroundColor Yellow -NoNewline
            $replace = Read-Host
            if ($replace.ToLower() -eq "s" -or $replace.ToLower() -eq "si" -or $replace.ToLower() -eq "y" -or $replace.ToLower() -eq "yes") {
                docker secret rm $secretName
                Write-Host "🗑️  Secret '$secretName' rimosso" -ForegroundColor Yellow
            } else {
                Write-Host "⏭️  Saltando secret '$secretName'" -ForegroundColor Yellow
                return
            }
        }
        
        # Crea il nuovo secret
        $secretValue | docker secret create $secretName -
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Secret '$secretName' creato con successo" -ForegroundColor Green
        } else {
            Write-Host "❌ Errore nella creazione del secret '$secretName'" -ForegroundColor Red
        }
    } else {
        Write-Host "⏭️  Saltando secret '$secretName' (valore vuoto)" -ForegroundColor Yellow
    }
}

# Crea i secrets
Write-Host "`n🔑 Creazione secrets..." -ForegroundColor Cyan

Create-DockerSecret "google_maps_api_key" $GoogleMapsApiKey
Create-DockerSecret "google_oauth_client_id" $GoogleOAuthClientId
Create-DockerSecret "google_oauth_client_secret" $GoogleOAuthClientSecret

# Lista i secrets creati
Write-Host "`n📋 Secrets attualmente configurati:" -ForegroundColor Cyan
docker secret ls

Write-Host "`n🎉 Configurazione completata!" -ForegroundColor Green
Write-Host "📝 Prossimi passi:" -ForegroundColor Yellow
Write-Host "   1. Verifica che tutti i secrets necessari siano elencati sopra" -ForegroundColor White
Write-Host "   2. Avvia l'applicazione con: docker-compose up -d" -ForegroundColor White
Write-Host "   3. Controlla i logs con: docker-compose logs -f spring-app" -ForegroundColor White

Write-Host "`n💡 Suggerimenti:" -ForegroundColor Cyan
Write-Host "   - Per aggiornare un secret, rimuovilo prima con: docker secret rm nome_secret" -ForegroundColor White
Write-Host "   - Per vedere tutti i secrets: docker secret ls" -ForegroundColor White
Write-Host "   - Per rimuovere tutti i secrets: docker secret rm `$(docker secret ls -q)" -ForegroundColor White
