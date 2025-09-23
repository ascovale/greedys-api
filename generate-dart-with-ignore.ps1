param(
  [string]$SpecPath = "..\common_lib\generator\swagger.json",
  [string]$TemplatesPath = "..\common_lib\generator\templates",
  [string]$OutDir = "greedys_api_dart_lib",
  [string]$IgnoreFile = "..\common_lib\generator\.openapi-generator-ignore",
  [string]$PubName = "openapi",
  [string]$PubVersion = "1.0.0",
  [string]$PubDescription = "OpenAPI API client",
  [string]$PubHomepage = "homepage",
  [string]$WrappersEndpoint = "https://api.greedys.it/api/internal/response-wrapper-catalog",
  [string]$WrappersFile = "..\common_lib\generator\response-wrappers-restaurant.json",
  [string]$Group = "restaurant" # Script specifico per il gruppo restaurant
)

# Determina il gruppo e aggiorna i percorsi di conseguenza
if ($Group -and $Group -ne "") {
  Write-Host "[INFO] Gruppo: $Group" -ForegroundColor Cyan
  # Aggiorna il file wrapper per usare il file specifico del gruppo
  $baseDir = Split-Path $WrappersFile -Parent
  $WrappersFile = Join-Path $baseDir "response-wrappers-$Group.json"
  Write-Host "[INFO] File wrapper specifico: $WrappersFile" -ForegroundColor DarkCyan
} else {
  Write-Host "[INFO] Nessun gruppo specificato, uso file wrapper globale" -ForegroundColor Yellow
}

# (Opzionale) Build custom generator JAR se esiste pom.xml
$customGenRoot = Resolve-Path "..\common_lib\generator" -ErrorAction SilentlyContinue
$customJar = $null
if ($customGenRoot) {
  $pom = Join-Path $customGenRoot "pom.xml"
  if (Test-Path $pom) {
    Write-Host "[INFO] Costruisco custom generator (Maven)..." -ForegroundColor Yellow
    pushd $customGenRoot
    mvn -q -DskipTests package 2>$null
    popd
    $candidate = Join-Path $customGenRoot "target\custom-dart-codegen-1.0.0-SNAPSHOT.jar"
    if (Test-Path $candidate) {
      $customJar = $candidate
      Write-Host "[OK] Custom generator costruito: $customJar" -ForegroundColor Green
    } else {
      Write-Host "[WARN] JAR custom non trovato dopo build" -ForegroundColor Yellow
    }
  }
}

Write-Host "[INFO] Avvio generazione Dart (Docker via WSL) con ignore override..." -ForegroundColor Yellow

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

if (-not (Test-Path $SpecPath)) { Write-Error "Spec non trovata: $SpecPath"; exit 1 }
if (-not (Test-Path $TemplatesPath)) { Write-Error "Templates non trovati: $TemplatesPath"; exit 1 }
if (-not (Test-Path $IgnoreFile)) { Write-Warning "Ignore file non trovato: $IgnoreFile (procedo comunque)" }

# Assicura la presenza del catalogo dei wrapper
if (Test-Path $WrappersFile) {
  Write-Host "[INFO] Catalogo wrapper già presente: $WrappersFile" -ForegroundColor DarkGreen
} else {
  # Determina l'endpoint corretto in base al gruppo
  $finalEndpoint = if ($Group -and $Group -ne "") {
    "$WrappersEndpoint/$Group"
  } else {
    $WrappersEndpoint
  }
  
  Write-Host "[INFO] Scarico catalogo wrapper da $finalEndpoint" -ForegroundColor Yellow
  try {
    $resp = Invoke-RestMethod -Uri $finalEndpoint -Method GET -TimeoutSec 10
    if (-not $resp) { throw "Risposta vuota" }
    ($resp | ConvertTo-Json -Depth 25) | Out-File -FilePath $WrappersFile -Encoding utf8
    Write-Host "[OK] Catalogo wrapper salvato in $WrappersFile" -ForegroundColor Green
  } catch {
    Write-Error "Impossibile scaricare il catalogo wrapper ($finalEndpoint): $($_.Exception.Message)"; exit 1
  }
}

# Pulisci output
if (Test-Path $OutDir) { Remove-Item -Recurse -Force $OutDir }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# Calcola percorsi WSL
$currentDir = (Get-Location).Path
$driveLetter = $currentDir.Substring(0,1).ToLower()
$restOfPath = $currentDir.Substring(2) -replace '\\', '/'
$wslPath = "/mnt/$driveLetter$restOfPath"

# Percorso WSL della cartella target del custom generator (sibling: ../common_lib/generator/target)
$parentPath = Split-Path -Parent $currentDir
$parentRest = $parentPath.Substring(2) -replace '\\','/'
$wslParent = "/mnt/$driveLetter$parentRest"
$wslCustomTarget = "$wslParent/common_lib/generator/target"

Write-Host "[INFO] Current: $currentDir" -ForegroundColor Gray
Write-Host "[INFO] WSL   : $wslPath" -ForegroundColor Gray

# Esegui generator con ignore override (usa più flag -p per evitare problemi di quoting)
if ($customJar) {
  # Prova prima con Java locale usando il custom generator
  Write-Host "[INFO] Provo Java locale con custom generator..." -ForegroundColor Yellow
  $toolsDir = Join-Path $scriptDir 'tools'
  if (-not (Test-Path $toolsDir)) { New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null }
  $cliJar = Join-Path $toolsDir 'openapi-generator-cli.jar'
  if (-not (Test-Path $cliJar)) {
    Write-Host "[INFO] Scarico openapi-generator-cli.jar..." -ForegroundColor Yellow
    $cliUrl = 'https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/7.8.0/openapi-generator-cli-7.8.0.jar'
    try {
      Invoke-WebRequest -Uri $cliUrl -OutFile $cliJar -UseBasicParsing -TimeoutSec 30
      Write-Host "[OK] Scaricato openapi-generator-cli.jar" -ForegroundColor Green
    } catch {
      Write-Error "Impossibile scaricare openapi-generator-cli.jar: $($_.Exception.Message)"; exit 1
    }
  }
  $cp = "$cliJar;$customJar"
  $specPath = Join-Path $scriptDir $SpecPath
  $outPath = Join-Path $scriptDir $OutDir
  $tplPath = Join-Path $scriptDir $TemplatesPath  
  $ignorePath = Join-Path $scriptDir $IgnoreFile
  $args = @(
    '-cp', $cp,
    'org.openapitools.codegen.OpenAPIGenerator',
    'generate',
    '-g', 'org.openapitools.custom.CustomDartClientCodegen',
    '-i', $specPath,
    '-o', $outPath,
    '-t', $tplPath,
    '--ignore-file-override', $ignorePath,
    '--skip-validate-spec',
    '-p', "pubName=$PubName",
    '-p', "pubVersion=$PubVersion", 
    '-p', "pubDescription=$PubDescription",
    '-p', "pubHomepage=$PubHomepage"
  )
  Write-Host "[CMD] java $($args -join ' ')" -ForegroundColor DarkGray
  & java @args
  if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Custom generator Java locale riuscito!" -ForegroundColor Green
  } else {
    Write-Host "[WARN] Custom generator Java locale fallito, uso Docker standard..." -ForegroundColor Yellow
    $generatorName = "dart-dio"
    # Comando docker come fallback
    wsl docker run --rm -v "${wslPath}:/local" -v "${wslCustomTarget}:/custom-gen" openapitools/openapi-generator-cli generate `
      -i "/local/$SpecPath" `
      -g $generatorName `
      -o "/local/$OutDir" `
      -t "/local/$TemplatesPath" `
      --ignore-file-override "/local/$IgnoreFile" `
      -p "pubName=$PubName" `
      -p "pubVersion=$PubVersion" `
      -p "pubDescription=$PubDescription" `
      -p "pubHomepage=$PubHomepage"
  }
} else {
  $generatorName = "dart-dio" # usa generator standard
  # Comando docker effettivo - sempre via WSL dato che Docker è solo su WSL
  wsl docker run --rm -v "${wslPath}:/local" -v "${wslCustomTarget}:/custom-gen" openapitools/openapi-generator-cli generate `
    -i "/local/$SpecPath" `
    -g $generatorName `
    -o "/local/$OutDir" `
    -t "/local/$TemplatesPath" `
    --ignore-file-override "/local/$IgnoreFile" `
    -p "pubName=$PubName" `
    -p "pubVersion=$PubVersion" `
    -p "pubDescription=$PubDescription" `
    -p "pubHomepage=$PubHomepage"
}

# NOTE: Rimosso la copia dell'ignore file nella cartella generata per evitare confusione
# Il file ignore in restaurant_app/ è quello che viene effettivamente utilizzato
# if (Test-Path $IgnoreFile) { Copy-Item $IgnoreFile "$OutDir\.openapi-generator-ignore" -Force }

# Rimuovi file di documentazione e test ResponseWrapper indesiderati
Write-Host "[INFO] Pulizia file ResponseWrapper (doc/test)..." -ForegroundColor Yellow
if (Test-Path "$OutDir\doc") {
  Get-ChildItem -Path "$OutDir\doc" -Filter "ResponseWrapper*.md" | Where-Object { $_.Name -ne "ResponseWrapperErrorDetails.md" } | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "  Rimosso: $($_.Name)" -ForegroundColor DarkGray
  }
}
if (Test-Path "$OutDir\test") {
  Get-ChildItem -Path "$OutDir\test" -Filter "response_wrapper*.dart" | Where-Object { $_.Name -ne "response_wrapper_error_details_test.dart" } | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "  Rimosso: $($_.Name)" -ForegroundColor DarkGray
  }
}

# Rimuovi import indesiderati verso i modelli ResponseWrapper generati (che vengono ignorati)
Write-Host "[INFO] Pulizia import ResponseWrapper generati negli API/model..." -ForegroundColor Yellow
$patterns = @(
  "^import 'package:openapi/src/model/response_wrapper.*';\s*$",
  '^insert' # placeholder to avoid trailing comma
)
$patterns = $patterns | Where-Object { $_ -ne '^insert' }

foreach ($dir in @("$OutDir\lib\src\api", "$OutDir\lib\src\model")) {
  if (-not (Test-Path $dir)) { continue }
  Get-ChildItem -Path $dir -Filter *.dart -Recurse | ForEach-Object {
    $file = $_.FullName
    $orig = Get-Content -LiteralPath $file -Raw
    $new = $orig
    foreach ($pat in $patterns) {
      $new = [System.Text.RegularExpressions.Regex]::Replace($new, $pat, '', 'Multiline')
    }
    if ($new -ne $orig) {
      # comprime righe vuote duplicate create dalla rimozione
      $new = [System.Text.RegularExpressions.Regex]::Replace($new, "\n{3,}", "`n`n")
      Set-Content -LiteralPath $file -Value $new -Encoding UTF8
      Write-Host "[CLEAN] $file" -ForegroundColor DarkGray
    }
  }
}

# Aggiungi dipendenza common_lib sotto 'dependencies' nel pubspec del pacchetto generato (ed eventualmente rimuovi da dev_dependencies)
if (Test-Path "$OutDir\pubspec.yaml") {
  Write-Host "[INFO] Allineo dipendenza common_lib nel pubspec.yaml (dependencies)..." -ForegroundColor Yellow
  $pubspecPath = Join-Path $OutDir 'pubspec.yaml'
  $content = Get-Content -LiteralPath $pubspecPath -Raw

  # Se già presente in dependencies, non fare nulla
  $alreadyInDeps = $content -match "(?ms)^dependencies:\s*[\s\S]*?^\s*common_lib:\s*\n\s*path:\s*\S+"
  if (-not $alreadyInDeps) {
    # Rimuovi eventuale blocco common_lib in dev_dependencies
    $content = [Regex]::Replace($content, "(?ms)^(dev_dependencies:\s*[\s\S]*?)(\n\s*common_lib:\s*\n\s*path:\s*\S+\s*)", '${1}', [System.Text.RegularExpressions.RegexOptions]::Multiline)

    # Inserisci sotto dependencies:
    if ($content -match "(?m)^dependencies:\s*$") {
      $content = [Regex]::Replace(
        $content,
        "(?m)^dependencies:\s*$",
        "dependencies:`n  common_lib:`n    path: ../../common_lib"
      )
    } else {
      # Se c'è già la sezione dependencies con contenuto, aggiungi la voce alla fine della sezione
      $content = [Regex]::Replace(
        $content,
        "(?ms)^(dependencies:\s*[\s\S]*?)(\n\w+?:)",
        { param($m) "${($m.Groups[1].Value).TrimEnd()}`n  common_lib:`n    path: ../../common_lib`n${$m.Groups[2].Value}" },
        1
      )
      if (-not ($content -match "(?ms)^dependencies:\s*[\s\S]*?\n\s*common_lib:")) {
        # Se non ha funzionato, appendi una sezione dependencies minimale
        $content += "`ndependencies:`n  common_lib:`n    path: ../../common_lib`n"
      }
    }

    Set-Content -LiteralPath $pubspecPath -Value $content -Encoding UTF8
    Write-Host "[OK] common_lib configurato in dependencies" -ForegroundColor Green
  } else {
    Write-Host "[SKIP] common_lib già presente in dependencies" -ForegroundColor DarkGray
  }
}

# Rimuovi eventuali export dei ResponseWrapper da lib/openapi.dart (safety net)
# EXCEPT ResponseWrapperErrorDetails which is needed for error handling
$openapiFile = Join-Path $OutDir 'lib\openapi.dart'
if (Test-Path $openapiFile) {
  $src = Get-Content -LiteralPath $openapiFile -Raw
  $clean = [System.Text.RegularExpressions.Regex]::Replace($src, "^export 'package:$PubName/src/model/response_wrapper(?!_error_details).*';\s*$", '', 'Multiline')
  if ($clean -ne $src) {
    $clean = [System.Text.RegularExpressions.Regex]::Replace($clean, "\n{3,}", "`n`n")
    Set-Content -LiteralPath $openapiFile -Value $clean -Encoding UTF8
    Write-Host "[CLEAN] openapi.dart: rimossi export ResponseWrapper*" -ForegroundColor DarkGray
  }
}


# Deduplica import post-process rimossa: gestita dal generatore custom

Write-Host "[OK] Generazione completata: $OutDir" -ForegroundColor Green