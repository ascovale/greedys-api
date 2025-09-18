# Script per esecuzione rapida profilo DEV

Write-Host "ROCKET Greedys API - Avvio profilo DEV" -ForegroundColor Green
Write-Host "CLIPBOARD Configurazione: MySQL locale + servizi reali (Firebase, Google, Twilio)" -ForegroundColor Yellow
Write-Host ""

# Verifica che MySQL locale sia in esecuzione
Write-Host "MAGNIFYING_GLASS Verifica MySQL locale..." -ForegroundColor Cyan

# Funzione per verificare e avviare MySQL locale
function Check-MySQLLocal {
    # Prima prova a connettersi
    try {
        $null = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -pMinosse100% -e "SELECT 1;" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "CHECK_MARK MySQL locale e gia attivo e raggiungibile!" -ForegroundColor Green
        } else {
            throw "MySQL non raggiungibile"
        }
    } catch {
        Write-Host "WARNING MySQL locale non e attivo. Tentativo di avvio..." -ForegroundColor Yellow
        
        # Prova ad avviare MySQL (Windows Services)
        try {
            # Prova con il servizio MySQL
            $service = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($service) {
                if ($service.Status -ne "Running") {
                    Write-Host "Avvio servizio MySQL..." -ForegroundColor Yellow
                    Start-Service $service.Name
                    Start-Sleep -Seconds 3
                }
            } else {
                Write-Host "X Non riesco a trovare il servizio MySQL" -ForegroundColor Red
                Write-Host "   Avvia MySQL manualmente e riprova" -ForegroundColor Red
                return $false
            }
        } catch {
            Write-Host "X Errore nell'avvio del servizio MySQL: $($_.Exception.Message)" -ForegroundColor Red
            return $false
        }
        
        # Verifica di nuovo dopo il tentativo di avvio
        Start-Sleep -Seconds 3
        try {
            $null = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -pMinosse100% -e "SELECT 1;" 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "CHECK_MARK MySQL locale avviato con successo!" -ForegroundColor Green
            } else {
                throw "MySQL ancora non raggiungibile"
            }
        } catch {
            Write-Host "X ERRORE: MySQL locale non e raggiungibile dopo il tentativo di avvio!" -ForegroundColor Red
            Write-Host "   Verifica che MySQL sia installato e che la password sia corretta" -ForegroundColor Red
            Write-Host "   Comando di test: `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`" -u root -pMinosse100% -e 'SELECT 1;'" -ForegroundColor Red
            return $false
        }
    }
        
    # Verifica/crea database greedys_dev
    try {
        $null = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -pMinosse100% -e "CREATE DATABASE IF NOT EXISTS greedys_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>$null
        Write-Host "CHECK_MARK Database greedys_dev verificato/creato!" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "WARNING Errore nella creazione del database, ma proseguo..." -ForegroundColor Yellow
        return $true
    }
}

# Verifica MySQL
if (Check-MySQLLocal) {
    Write-Host "TARGET MySQL locale pronto per l'applicazione!" -ForegroundColor Green
} else {
    Write-Host "WARNING Proseguo comunque, ma potrebbero esserci errori di connessione..." -ForegroundColor Yellow
}
Write-Host ""

# Funzione per menu
function Show-Menu {
    Write-Host "Scegli modalità di esecuzione DEV:" -ForegroundColor Cyan
    Write-Host "1) dev - Avvio standard (MySQL + servizi reali)" -ForegroundColor White
    Write-Host "2) dev-minimal - Avvio ULTRA VELOCE (H2 memoria + mock services)" -ForegroundColor Green
    Write-Host "3) dev con hot reload (ricaricamento automatico)" -ForegroundColor White
    Write-Host "4) Solo compilazione (progress dettagliato)" -ForegroundColor White
    Write-Host "5) Pulisci cache Maven" -ForegroundColor White
    Write-Host "6) Avvio veloce (usa jar esistente, no rebuild)" -ForegroundColor White
    Write-Host "7) Compilazione VERBOSE (mostra tutti i dettagli)" -ForegroundColor White
    Write-Host "8) Ferma MySQL (Docker + locale) e esci" -ForegroundColor White
    Write-Host ""
    $choice = Read-Host "Inserisci numero (1-8)"
    return $choice
}

# Funzione per fermare MySQL
function Stop-MySQL {
    Write-Host "STOP_SIGN Fermo MySQL..." -ForegroundColor Yellow
    
    # Ferma MySQL Docker se presente
    $dockerAvailable = $false
    try {
        docker --version 2>$null | Out-Null
        $dockerAvailable = $true
    } catch {
        Write-Host "INFO Docker non disponibile" -ForegroundColor Blue
    }
    
    if ($dockerAvailable) {
        $containers = docker ps --format "table {{.Names}}" 2>$null
        if ($containers -and ($containers -match "greedys-mysql-dev")) {
            docker stop greedys-mysql-dev
            docker rm greedys-mysql-dev
            Write-Host "CHECK_MARK MySQL Docker fermato e rimosso" -ForegroundColor Green
        } else {
            Write-Host "INFO MySQL Docker non era in esecuzione" -ForegroundColor Blue
        }
    }
    
    # Ferma MySQL locale (servizio Windows)
    Write-Host "INFO Tentativo di fermare MySQL locale..." -ForegroundColor Cyan
    try {
        # Prova i nomi di servizio MySQL più comuni
        $mysqlServices = @("MySQL80", "MySQL", "mysql", "MySql")
        $serviceStopped = $false
        
        foreach ($serviceName in $mysqlServices) {
            $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
            if ($service -and $service.Status -eq "Running") {
                Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
                Write-Host "CHECK_MARK MySQL locale ($serviceName) fermato" -ForegroundColor Green
                $serviceStopped = $true
                break
            }
        }
        
        if (-not $serviceStopped) {
            Write-Host "INFO MySQL locale non trovato o non in esecuzione" -ForegroundColor Blue
        }
    } catch {
        Write-Host "WARNING Non riesco a fermare MySQL locale (potrebbero servire privilegi admin)" -ForegroundColor Yellow
        Write-Host "INFO Per fermare manualmente: 'net stop mysql80' (come amministratore)" -ForegroundColor Gray
    }
}

# Funzione per esecuzione rapida DEV
function Quick-DevRun {
    Write-Host "LIGHTNING Esecuzione rapida profilo DEV" -ForegroundColor Green
    Write-Host "CLIPBOARD Flags di ottimizzazione:" -ForegroundColor Yellow
    Write-Host "  - Profilo Maven: FULL (tutte le dipendenze)" -ForegroundColor White
    Write-Host "  - Profilo Spring: dev (MySQL + servizi reali)" -ForegroundColor White
    Write-Host "  - Skip tests (-DskipTests -Dmaven.test.skip=true)" -ForegroundColor White
    Write-Host "  - MySQL pronto e testato OK" -ForegroundColor White
    Write-Host ""
    Write-Host "HAMMER Avvio compilazione e esecuzione..." -ForegroundColor Cyan
    Write-Host "   (Questo potrebbe richiedere alcuni minuti al primo avvio)" -ForegroundColor Gray
    Write-Host ""
    
    Set-Location "greedys_api"
    & mvn spring-boot:run `
        "-Pfull" `
        "-Dspring.profiles.active=dev" `
        "-Dspring-boot.run.profiles=dev" `
        "-DskipTests" `
        "-Dmaven.test.skip=true" `
        "-Dspring-boot.run.fork=false" `
        "--batch-mode" `
        "--show-version" `
        "-o" `
        "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
}

# Funzione per esecuzione ULTRA VELOCE dev-minimal 
function UltraFast-DevRun {
    Write-Host "ROCKET ULTRA VELOCE - Profilo dev-minimal" -ForegroundColor Green
    Write-Host "CLIPBOARD Configurazione ottimizzata per velocita MASSIMA:" -ForegroundColor Yellow
    Write-Host "  - Database: H2 in memoria (no MySQL, no connessioni esterne)" -ForegroundColor White
    Write-Host "  - Servizi: TUTTI MOCK (Firebase, Google, Twilio disabilitati)" -ForegroundColor White
    Write-Host "  - Profilo Spring: dev-minimal" -ForegroundColor White
    Write-Host "  - Logging: MINIMAL (solo errori critici)" -ForegroundColor White
    Write-Host "  - HOT RELOAD: Attivo (ricaricamento automatico + LiveReload)" -ForegroundColor Green
    Write-Host "  - Startup: < 30 secondi FIRE" -ForegroundColor White
    Write-Host ""
    Write-Host "WARNING NOTA: Questo profilo e solo per sviluppo rapido!" -ForegroundColor Yellow
    Write-Host "   - Nessun dato persistente (H2 in memoria)" -ForegroundColor Gray
    Write-Host "   - Servizi esterni simulati (mock responses)" -ForegroundColor Gray
    Write-Host "   - Non adatto per test di integrazione" -ForegroundColor Gray
    Write-Host ""
    Write-Host "HAMMER Avvio ULTRA-veloce con HOT RELOAD..." -ForegroundColor Cyan
    Write-Host ""
    
    Set-Location "greedys_api"
    & mvn spring-boot:run `
        "-Pminimal" `
        "-Dspring.profiles.active=dev-minimal" `
        "-Dspring-boot.run.profiles=dev-minimal" `
        "-DskipTests" `
        "-Dmaven.test.skip=true" `
        "-Dspring-boot.run.fork=true" `
        "-Dspring.devtools.restart.enabled=true" `
        "-Dspring.devtools.livereload.enabled=true" `
        "-Dspring.devtools.restart.additional-paths=src/main/java" `
        "-Dspring.devtools.restart.poll-interval=1000" `
        "-Dspring.devtools.restart.quiet-period=400" `
        "--batch-mode" `
        "--show-version" `
        "-o" `
        "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
}

# Funzione per esecuzione con hot reload
function HotReload-DevRun {
    Write-Host "FIRE Hot reload attivo profilo DEV" -ForegroundColor Red
    Write-Host "CLIPBOARD Features:" -ForegroundColor Yellow
    Write-Host "  - Ricaricamento automatico classi modificate" -ForegroundColor White
    Write-Host "  - LiveReload per browser" -ForegroundColor White
    Write-Host "  - Restart veloce su modifiche" -ForegroundColor White
    Write-Host "  - MySQL pronto e testato OK" -ForegroundColor White
    Write-Host ""
    Write-Host "HAMMER Avvio compilazione con hot reload..." -ForegroundColor Cyan
    Write-Host "   (Questo potrebbe richiedere alcuni minuti al primo avvio)" -ForegroundColor Gray
    Write-Host ""
    
    Set-Location "greedys_api"
    & mvn spring-boot:run `
        "-Pfull" `
        "-Dspring.profiles.active=dev" `
        "-Dspring-boot.run.profiles=dev" `
        "-DskipTests" `
        "-Dmaven.test.skip=true" `
        "-Dspring-boot.run.fork=true" `
        "-Dspring.devtools.restart.enabled=true" `
        "-Dspring.devtools.livereload.enabled=true" `
        "-Dspring.devtools.restart.additional-paths=src/main/java" `
        "-Dspring.devtools.restart.poll-interval=1000" `
        "-Dspring.devtools.restart.quiet-period=400" `
        "--batch-mode" `
        "--show-version" `
        "-o" `
        "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
}

# Funzione per solo compilazione
function Compile-Only {
    Write-Host "HAMMER Solo compilazione..." -ForegroundColor Cyan
    Write-Host "   (Mostra progresso dettagliato)" -ForegroundColor Gray
    Write-Host ""
    Set-Location "greedys_api"
    & mvn compile `
        "-DskipTests" `
        "-Dmaven.test.skip=true" `
        "--batch-mode" `
        "--show-version" `
        "-o" `
        "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn"
}

# Funzione per pulizia cache
function Clean-Cache {
    Write-Host "BROOM Pulizia cache Maven..." -ForegroundColor Yellow
    Set-Location "greedys_api"
    & mvn clean
    Write-Host "CHECK_MARK Cache pulita!" -ForegroundColor Green
}

# Funzione per compilazione verbose
function Verbose-Compile {
    Write-Host "SPEAKER Compilazione VERBOSE - Mostra tutti i dettagli" -ForegroundColor Magenta
    Write-Host "CLIPBOARD Questo mostrera:" -ForegroundColor Yellow
    Write-Host "  - Progress di ogni singolo file compilato" -ForegroundColor White
    Write-Host "  - Download dipendenze con progress" -ForegroundColor White
    Write-Host "  - Dettagli completi del processo Maven" -ForegroundColor White
    Write-Host ""
    Write-Host "WARNING ATTENZIONE: Output molto verboso!" -ForegroundColor Red
    Write-Host ""
    Set-Location "greedys_api"
    & mvn compile `
        "-DskipTests" `
        "-Dmaven.test.skip=true" `
        "--debug" `
        "--show-version" `
        "-X"
}

# Funzione per avvio veloce senza rebuild
function Quick-JarRun {
    Write-Host "LIGHTNING Avvio veloce senza rebuild" -ForegroundColor Green
    Write-Host "CLIPBOARD Usa il jar gia compilato in target/" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Scegli profilo Spring:" -ForegroundColor Cyan
    Write-Host "1) dev (MySQL + servizi reali)" -ForegroundColor White
    Write-Host "2) dev-minimal (H2 + mock services)" -ForegroundColor Green
    $profileChoice = Read-Host "Profilo (1-2)"
    
    Set-Location "greedys_api"
    
    # Controlla se esiste il jar
    if (-not (Test-Path "target/greedys_api-0.1.1.jar")) {
        Write-Host "X JAR non trovato! Compila prima con l'opzione 4" -ForegroundColor Red
        Write-Host "   Oppure usa l'opzione 1 per compilare e avviare" -ForegroundColor Red
        return
    }
    
    switch ($profileChoice) {
        "1" {
            Write-Host "ROCKET Avvio JAR esistente con profilo DEV..." -ForegroundColor Green
            & java -jar target/greedys_api-0.1.1.jar "--spring.profiles.active=dev"
        }
        "2" {
            Write-Host "ROCKET Avvio JAR esistente con profilo DEV-MINIMAL..." -ForegroundColor Green
            & java -jar target/greedys_api-0.1.1.jar "--spring.profiles.active=dev-minimal"
        }
        default {
            Write-Host "X Scelta profilo non valida!" -ForegroundColor Red
        }
    }
}

# Menu principale
$choice = Show-Menu

switch ($choice) {
    "1" {
        Quick-DevRun
    }
    "2" {
        UltraFast-DevRun
    }
    "3" {
        HotReload-DevRun
    }
    "4" {
        Compile-Only
    }
    "5" {
        Clean-Cache
    }
    "6" {
        Quick-JarRun
    }
    "7" {
        Verbose-Compile
    }
    "8" {
        Stop-MySQL
    }
    default {
        Write-Host "X Scelta non valida!" -ForegroundColor Red
    }
}
