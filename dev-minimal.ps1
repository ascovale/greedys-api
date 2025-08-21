# ============================================================================
# GREEDYS API - DEV MINIMAL LAUNCHER (PowerShell)
# Script PowerShell per avvio ultra-veloce modalità sviluppo minimal
# ============================================================================

Write-Host "🚀 ===== GREEDYS API - DEV MINIMAL LAUNCHER =====" -ForegroundColor Cyan
Write-Host ""

# Imposta directory progetto
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiDir = Join-Path $ProjectRoot "greedys_api"

Write-Host "📁 Directory progetto: $ApiDir" -ForegroundColor Yellow
Write-Host "⚡ Modalità: ULTRA-VELOCE (minimal profile)" -ForegroundColor Green
Write-Host ""

# Controlla se directory esiste
if (-not (Test-Path $ApiDir)) {
    Write-Host "❌ Directory greedys_api non trovata!" -ForegroundColor Red
    Write-Host "   Assicurati di essere nella directory root del progetto" -ForegroundColor Yellow
    exit 1
}

# Cambia directory
Set-Location $ApiDir

# Verifica Java e Maven
Write-Host "🔍 Verifica ambiente..." -ForegroundColor Yellow

try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "   ☕ Java: $($javaVersion.Line)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Java non trovato! Installa Java 17+" -ForegroundColor Red
    exit 1
}

try {
    $mavenVersion = mvn -version | Select-String "Apache Maven"
    Write-Host "   📦 Maven: $($mavenVersion.Line)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Maven non trovato! Installa Maven 3.6+" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Configurazione JVM ottimizzata per Windows
Write-Host "⚙️  Configurazione JVM ottimizzata..." -ForegroundColor Yellow
$env:MAVEN_OPTS = "-Xmx2g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.awt.headless=true"
Write-Host "   💾 Memory: 2GB max, 512MB start" -ForegroundColor Green
Write-Host "   🗑️  GC: G1 with String Deduplication" -ForegroundColor Green
Write-Host ""

# Verifica file di configurazione
$configFile = "src\main\resources\application-dev-minimal.properties"
if (Test-Path $configFile) {
    Write-Host "✅ File configurazione trovato: $configFile" -ForegroundColor Green
} else {
    Write-Host "⚠️  File configurazione non trovato: $configFile" -ForegroundColor Yellow
}

# Verifica mock services
Write-Host "🔍 Verifica mock services..." -ForegroundColor Yellow
$mockFiles = @(
    "src\main\java\com\application\common\spring\MockFirebaseService.java",
    "src\main\java\com\application\common\spring\MockGoogleAuthService.java",
    "src\main\java\com\application\common\spring\MockGooglePlacesSearchService.java",
    "src\main\java\com\application\common\spring\MockTwilioConfig.java",
    "src\main\java\com\application\common\spring\MockReliableNotificationService.java"
)

$mockCount = 0
foreach ($file in $mockFiles) {
    if (Test-Path $file) {
        $mockCount++
        $fileName = Split-Path $file -Leaf
        Write-Host "   ✅ $fileName" -ForegroundColor Green
    } else {
        $fileName = Split-Path $file -Leaf
        Write-Host "   ❌ $fileName - MISSING" -ForegroundColor Red
    }
}

Write-Host "   📊 Mock services trovati: $mockCount/5" -ForegroundColor $(if ($mockCount -eq 5) { "Green" } else { "Yellow" })
Write-Host ""

# Opzioni di avvio
Write-Host "🎯 OPZIONI AVVIO:" -ForegroundColor Cyan
Write-Host "   1. 🏃 QUICK START - Compila e avvia (raccomandato)" -ForegroundColor White
Write-Host "   2. 📦 COMPILE ONLY - Solo compilazione" -ForegroundColor White  
Write-Host "   3. 🚀 RUN ONLY - Solo avvio (se già compilato)" -ForegroundColor White
Write-Host "   4. 🧹 CLEAN BUILD - Pulizia completa e ricompilazione" -ForegroundColor White
Write-Host "   5. ❌ EXIT - Esci" -ForegroundColor White
Write-Host ""

do {
    $choice = Read-Host "Scegli opzione (1-5)"
    
    switch ($choice) {
        "1" {
            Write-Host ""
            Write-Host "🏃 QUICK START - Compila e avvia..." -ForegroundColor Cyan
            Write-Host "📦 Comando: mvn clean compile spring-boot:run -Pminimal" -ForegroundColor Yellow
            Write-Host "⏱️  Tempo stimato: 1-3 minuti" -ForegroundColor Yellow
            Write-Host ""
            
            $startTime = Get-Date
            Write-Host "⏰ Inizio: $($startTime.ToString('HH:mm:ss'))" -ForegroundColor Green
            
            # Esecuzione Maven
            mvn clean compile spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal -DskipTests
            
            $endTime = Get-Date
            $duration = $endTime - $startTime
            Write-Host ""
            Write-Host "⏰ Fine: $($endTime.ToString('HH:mm:ss'))" -ForegroundColor Green
            Write-Host "⏱️  Durata totale: $($duration.ToString('mm\:ss'))" -ForegroundColor Cyan
            break
        }
        
        "2" {
            Write-Host ""
            Write-Host "📦 COMPILE ONLY - Solo compilazione..." -ForegroundColor Cyan
            Write-Host "📦 Comando: mvn clean compile -Pminimal" -ForegroundColor Yellow
            Write-Host ""
            
            $startTime = Get-Date
            mvn clean compile -Pminimal
            $endTime = Get-Date
            $duration = $endTime - $startTime
            
            Write-Host ""
            Write-Host "✅ Compilazione completata in $($duration.ToString('mm\:ss'))" -ForegroundColor Green
            Write-Host "🚀 Per avviare: mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal" -ForegroundColor Yellow
            break
        }
        
        "3" {
            Write-Host ""
            Write-Host "🚀 RUN ONLY - Solo avvio..." -ForegroundColor Cyan
            Write-Host "📦 Comando: mvn spring-boot:run -Pminimal" -ForegroundColor Yellow
            Write-Host ""
            
            mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal -DskipTests
            break
        }
        
        "4" {
            Write-Host ""
            Write-Host "🧹 CLEAN BUILD - Pulizia completa..." -ForegroundColor Cyan
            Write-Host "📦 Comando: mvn clean install -Pminimal" -ForegroundColor Yellow
            Write-Host "⏱️  Tempo stimato: 3-5 minuti (download dipendenze)" -ForegroundColor Yellow
            Write-Host ""
            
            $startTime = Get-Date
            mvn clean install -Pminimal -DskipTests
            $endTime = Get-Date
            $duration = $endTime - $startTime
            
            Write-Host ""
            Write-Host "✅ Build completo in $($duration.ToString('mm\:ss'))" -ForegroundColor Green
            
            # Avvia automaticamente dopo build
            Write-Host "🚀 Avvio automatico..." -ForegroundColor Cyan
            mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal -DskipTests
            break
        }
        
        "5" {
            Write-Host ""
            Write-Host "👋 Arrivederci!" -ForegroundColor Yellow
            exit 0
        }
        
        default {
            Write-Host "❌ Opzione non valida. Scegli 1-5." -ForegroundColor Red
        }
    }
} while ($choice -notin @("1", "2", "3", "4", "5"))

Write-Host ""
Write-Host "🎉 ===== DEV MINIMAL LAUNCHER COMPLETATO =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "📱 URL Disponibili (dopo avvio):" -ForegroundColor Yellow
Write-Host "   🌐 API: http://localhost:8080" -ForegroundColor White
Write-Host "   📖 Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "   💾 H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "     └─ JDBC URL: jdbc:h2:mem:greedys_dev" -ForegroundColor Gray
Write-Host "     └─ User: sa, Password: (vuoto)" -ForegroundColor Gray
Write-Host ""
Write-Host "🔧 Mock Services Attivi:" -ForegroundColor Yellow
Write-Host "   🔧 MOCK: Firebase, Google Auth, Google Places, Twilio, Notifications" -ForegroundColor Green
Write-Host ""
Write-Host "⌨️  Per fermare: Ctrl+C" -ForegroundColor Yellow
