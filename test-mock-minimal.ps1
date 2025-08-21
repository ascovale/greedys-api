# ============================================================================
# GREEDYS API - QUICK TEST MOCK SERVICES (PowerShell)
# Test veloce per verificare mock services
# ============================================================================

Write-Host "🧪 ===== QUICK TEST MOCK SERVICES =====" -ForegroundColor Cyan
Write-Host ""

# Directory progetto
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiDir = Join-Path $ProjectRoot "greedys_api"

Write-Host "📁 Directory: $ApiDir" -ForegroundColor Yellow
Set-Location $ApiDir

# Test 1: Verifica file mock
Write-Host "1️⃣ Verifica file mock services..." -ForegroundColor Cyan
$mockFiles = @(
    "src\main\java\com\application\common\spring\MockFirebaseService.java",
    "src\main\java\com\application\common\spring\MockGoogleAuthService.java", 
    "src\main\java\com\application\common\spring\MockGooglePlacesSearchService.java",
    "src\main\java\com\application\common\spring\MockTwilioConfig.java",
    "src\main\java\com\application\common\spring\MockReliableNotificationService.java"
)

$foundFiles = 0
foreach ($file in $mockFiles) {
    if (Test-Path $file) {
        $foundFiles++
        $fileName = Split-Path $file -Leaf
        Write-Host "   ✅ $fileName" -ForegroundColor Green
    } else {
        $fileName = Split-Path $file -Leaf  
        Write-Host "   ❌ $fileName - MISSING" -ForegroundColor Red
    }
}

# Test 2: Verifica configurazione
Write-Host ""
Write-Host "2️⃣ Verifica configurazione..." -ForegroundColor Cyan
$configFile = "src\main\resources\application-dev-minimal.properties"
if (Test-Path $configFile) {
    Write-Host "   ✅ $configFile" -ForegroundColor Green
    
    # Verifica proprietà chiave
    $content = Get-Content $configFile -Raw
    $properties = @(
        "firebase.enabled=false",
        "google.oauth.enabled=false", 
        "google.maps.enabled=false",
        "twilio.enabled=false",
        "notifications.enabled=false"
    )
    
    foreach ($prop in $properties) {
        if ($content -match [regex]::Escape($prop)) {
            Write-Host "   ✅ $prop" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  $prop - not found" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ❌ $configFile - MISSING" -ForegroundColor Red
}

# Test 3: Verifica profili Maven
Write-Host ""
Write-Host "3️⃣ Verifica profili Maven..." -ForegroundColor Cyan
if (Test-Path "pom.xml") {
    $pomContent = Get-Content "pom.xml" -Raw
    if ($pomContent -match "minimal") {
        Write-Host "   ✅ Profilo 'minimal' trovato" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Profilo 'minimal' non trovato" -ForegroundColor Red
    }
} else {
    Write-Host "   ❌ pom.xml non trovato" -ForegroundColor Red
}

# Test 4: Test veloce compilazione (opzionale)
Write-Host ""
Write-Host "4️⃣ Test compilazione veloce..." -ForegroundColor Cyan
$testCompile = Read-Host "Vuoi testare compilazione? (s/n)"

if ($testCompile -eq "s" -or $testCompile -eq "S") {
    Write-Host "   📦 Test compilazione minimal..." -ForegroundColor Yellow
    Write-Host "   ⏱️  Configurazione JVM ottimizzata..." -ForegroundColor Yellow
    
    # JVM ottimizzato per test veloce
    $env:MAVEN_OPTS = "-Xmx1g -Xms256m -XX:+UseG1GC"
    
    $startTime = Get-Date
    $result = Start-Process "mvn" -ArgumentList "compile", "-Pminimal", "-q" -Wait -PassThru -NoNewWindow
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    if ($result.ExitCode -eq 0) {
        Write-Host "   ✅ Compilazione OK in $($duration.ToString('mm\:ss'))" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Compilazione fallita in $($duration.ToString('mm\:ss'))" -ForegroundColor Red
    }
} else {
    Write-Host "   ⏭️  Test compilazione saltato" -ForegroundColor Yellow
}

# Risultati
Write-Host ""
Write-Host "🎯 ===== RISULTATI =====" -ForegroundColor Cyan
Write-Host "📊 Mock files trovati: $foundFiles/5" -ForegroundColor $(if ($foundFiles -eq 5) { "Green" } else { "Yellow" })

if ($foundFiles -eq 5) {
    Write-Host ""
    Write-Host "✅ MOCK SERVICES PRONTI!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Per avviare dev minimal:" -ForegroundColor Yellow
    Write-Host "   .\dev-minimal.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 Oppure direttamente:" -ForegroundColor Yellow  
    Write-Host "   mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "⚠️  Alcuni mock services mancano" -ForegroundColor Yellow
    Write-Host "🔧 Controlla implementazione" -ForegroundColor Yellow
}
