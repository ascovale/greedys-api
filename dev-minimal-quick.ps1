# ============================================================================
# GREEDYS API - DEV MINIMAL QUICK START (PowerShell)
# Avvio veloce per sviluppo minimal senza emoji
# ============================================================================

Write-Host "=== GREEDYS API - DEV MINIMAL QUICK START ===" -ForegroundColor Cyan
Write-Host ""

# Directory progetto
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiDir = Join-Path $ProjectRoot "greedys_api"

Write-Host "Directory: $ApiDir" -ForegroundColor Yellow
Set-Location $ApiDir

# Verifica ambiente
Write-Host "Verifica ambiente..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "Java OK: $($javaVersion.Line)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Java non trovato!" -ForegroundColor Red
    exit 1
}

try {
    $mavenVersion = mvn -version | Select-String "Apache Maven"
    Write-Host "Maven OK: $($mavenVersion.Line)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Maven non trovato!" -ForegroundColor Red
    exit 1
}

# Configurazione JVM ottimizzata
Write-Host ""
Write-Host "Configurazione JVM ottimizzata per Windows..." -ForegroundColor Yellow
$env:MAVEN_OPTS = "-Xmx2g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication"
Write-Host "MAVEN_OPTS: $env:MAVEN_OPTS" -ForegroundColor Green

# Test veloce presenza file
Write-Host ""
Write-Host "Quick check mock services..." -ForegroundColor Yellow
$mockFiles = @(
    "src\main\java\com\application\common\spring\MockFirebaseService.java",
    "src\main\java\com\application\common\spring\MockTwilioConfig.java"
)

$foundFiles = 0
foreach ($file in $mockFiles) {
    if (Test-Path $file) {
        $foundFiles++
        Write-Host "OK: $(Split-Path $file -Leaf)" -ForegroundColor Green
    }
}

Write-Host "Mock files trovati: $foundFiles" -ForegroundColor Green

# Menu semplice
Write-Host ""
Write-Host "OPZIONI:" -ForegroundColor Cyan
Write-Host "1. AVVIO VELOCE (compile + run)" -ForegroundColor White
Write-Host "2. SOLO COMPILAZIONE" -ForegroundColor White
Write-Host "3. SOLO RUN" -ForegroundColor White
Write-Host "4. EXIT" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Scegli (1-4)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "AVVIO VELOCE - Compile + Run..." -ForegroundColor Cyan
        Write-Host "Comando: mvn clean compile spring-boot:run -Pminimal" -ForegroundColor Yellow
        Write-Host ""
        
        $startTime = Get-Date
        & mvn clean compile spring-boot:run "-Pminimal" "-Dspring.profiles.active=dev-minimal" "-DskipTests"
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Host "Durata: $($duration.ToString('mm\:ss'))" -ForegroundColor Cyan
    }
    
    "2" {
        Write-Host ""
        Write-Host "SOLO COMPILAZIONE..." -ForegroundColor Cyan
        $startTime = Get-Date
        & mvn clean compile "-Pminimal"
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Host ""
        Write-Host "Compilazione completata in $($duration.ToString('mm\:ss'))" -ForegroundColor Green
    }
    
    "3" {
        Write-Host ""
        Write-Host "SOLO RUN..." -ForegroundColor Cyan
        & mvn spring-boot:run "-Pminimal" "-Dspring.profiles.active=dev-minimal" "-DskipTests"
    }
    
    "4" {
        Write-Host "Arrivederci!" -ForegroundColor Yellow
        exit 0
    }
    
    default {
        Write-Host "Opzione non valida" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== COMPLETATO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "URL Disponibili:" -ForegroundColor Yellow
Write-Host "API: http://localhost:8080" -ForegroundColor White
Write-Host "Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host ""
Write-Host "Per fermare: Ctrl+C" -ForegroundColor Yellow
