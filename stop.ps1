# Script PowerShell per fermare l'app Greedys
# Autore: Script automatico
# Data: $(Get-Date)
#
# Uso:
#   .\stop.ps1                    - Ferma l'app (mantiene DB)
#   .\stop.ps1 -db                - Ferma l'app e rimuove anche DB + volume

param(
    [switch]$db,
    [switch]$help
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
    Write-Host "Uso dello script stop:" -ForegroundColor Blue
    Write-Host "  .\stop.ps1                    - Ferma l'app (mantiene DB)"
    Write-Host "  .\stop.ps1 -db                - Ferma l'app e rimuove anche DB + volume"
    Write-Host "  .\stop.ps1 -help              - Mostra questo aiuto"
}

# Funzione per fermare i servizi
function Stop-Services {
    param([bool]$RemoveDb = $false)
    
    if ($RemoveDb) {
        Write-Warning "Modalità completa: rimuoverò anche DB e volume!"
        $confirmation = Read-Host "Sei sicuro? (y/N)"
        if ($confirmation -notmatch "^[Yy]$") {
            Write-Status "Operazione annullata dall'utente"
            exit 0
        }
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
        
        # Pulizia aggiuntiva - rimuove eventuali container orfani
        Write-Status "Pulizia container orfani..."
        try {
            docker container prune -f >$null 2>&1
            Write-Success "Container orfani rimossi!"
        } catch {
            # Ignora errori di pulizia
        }
        
        # Pulizia network orfani
        Write-Status "Pulizia network orfani..."
        try {
            docker network prune -f >$null 2>&1
            Write-Success "Network orfani rimossi!"
        } catch {
            # Ignora errori di pulizia
        }
        
        Write-Success "Pulizia completa terminata!"
    }
    
    Write-Success "Operazione di stop completata!"
}

# Gestione parametri
if ($help) {
    Write-Help
    exit 0
} else {
    Stop-Services -RemoveDb $db
}
