# Script PowerShell per avviare velocemente l'app Greedys
# Autore: Script automatico
# Data: $(Get-Date)
#
# Uso:
#   .\quick-start.ps1                    - Avvia l'app
#   .\quick-start.ps1 stop               - Ferma l'app (mantiene DB)
#   .\quick-start.ps1 stop -db           - Ferma l'app e rimuove anche DB + volume

param(
    [string]$Action = "",
    [switch]$db
)

# Funzione per stampare messaggi colorati
function Write-Status {
    param($Message)
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success {
    param($Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning {
    param($Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param($Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Help {
    Write-Host "Uso dello script:" -ForegroundColor Blue
    Write-Host "  .\quick-start.ps1                    - Avvia l'app"
    Write-Host "  .\quick-start.ps1 stop               - Ferma l'app (mantiene DB)"
    Write-Host "  .\quick-start.ps1 stop -db           - Ferma l'app e rimuove anche DB + volume"
}

# Funzione per fermare i servizi
function Stop-Services {
    param([bool]$RemoveDb = $false)
    
    if ($RemoveDb) {
        Write-Warning "Modalità completa: rimuoverò anche DB e volume!"
    }
    
    Write-Status "Arresto dello stack greedys_api..."
    
    try {
        docker stack rm greedys_api
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Stack greedys_api rimosso con successo!"
        } else {
            throw "Errore durante la rimozione dello stack"
        }
    } catch {
        Write-Error "Errore durante la rimozione dello stack"
        exit 1
    }
    
    if ($RemoveDb) {
        Write-Status "Attesa completamento rimozione servizi..."
        
        # Attende che tutti i servizi siano effettivamente rimossi
        do {
            Start-Sleep -Seconds 2
            $services = docker service ls --format "{{.Name}}" | Where-Object { $_ -like "*greedys_api*" }
            if ($services) {
                Write-Status "Attendo la rimozione completa dei servizi..."
            }
        } while ($services)
        
        Write-Status "Rimozione dello stack del database..."
        try {
            docker stack rm greedys_api_db 2>$null
            Write-Success "Stack database rimosso!"
        } catch {
            Write-Warning "Stack database non trovato o già rimosso"
        }
        
        # Attende che il database sia completamente rimosso
        Write-Status "Attesa rimozione completa database..."
        do {
            Start-Sleep -Seconds 2
            $dbServices = docker service ls --format "{{.Name}}" | Where-Object { $_ -like "*greedys_api_db*" }
            if ($dbServices) {
                Write-Status "Attendo la rimozione completa del database..."
            }
        } while ($dbServices)
        
        Write-Status "Rimozione volume database..."
        try {
            docker volume rm -f greedys_api_db_data 2>$null
            Write-Success "Volume database rimosso!"
        } catch {
            Write-Warning "Volume database non trovato o già rimosso"
        }
        
        Write-Success "Pulizia completa terminata!"
    }
    
    Write-Success "Operazione di stop completata!"
    exit 0
}

# Directory del progetto
$ProjectDir = "c:\Users\ascol\Progetti\greedys_api"

# Gestione parametri
if ($Action -eq "stop") {
    Stop-Services -RemoveDb $db
} elseif ($Action -eq "help" -or $Action -eq "-h" -or $Action -eq "--help") {
    Write-Help
    exit 0
} elseif ($Action -ne "") {
    Write-Error "Parametro non riconosciuto: $Action"
    Write-Help
    exit 1
}

# Se arriviamo qui, stiamo facendo il start
Write-Status "Avvio script di deployment per Greedys API..."
Write-Status "Directory progetto: $ProjectDir"

# Cambia nella directory del progetto
Write-Status "Cambio nella directory del progetto..."
try {
    Set-Location -Path $ProjectDir -ErrorAction Stop
    Write-Success "Directory cambiata correttamente"
} catch {
    Write-Error "Impossibile accedere alla directory $ProjectDir"
    exit 1
}

# Build dell'immagine Docker
Write-Status "Inizio build dell'immagine Docker..."
Write-Warning "Questo processo potrebbe richiedere alcuni minuti..."

try {
    docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest .
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Build dell'immagine completata con successo!"
    } else {
        throw "Errore durante la build"
    }
} catch {
    Write-Error "Errore durante la build dell'immagine Docker"
    exit 1
}

# Deploy dello stack
Write-Status "Deploy dello stack Docker..."

try {
    docker stack deploy -c docker-compose.yml greedys_api
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Stack deployato con successo!"
    } else {
        throw "Errore durante il deploy"
    }
} catch {
    Write-Error "Errore durante il deploy dello stack"
    exit 1
}

# Attesa per stabilizzazione dei servizi
Write-Status "Attesa stabilizzazione servizi (5 secondi)..."
Start-Sleep -Seconds 5

# Mostra i logs del servizio
Write-Status "Avvio monitoraggio logs del servizio spring-app..."
Write-Warning "Premi Ctrl+C per interrompere il monitoraggio dei logs"

docker service logs greedys_api_spring-app -f
