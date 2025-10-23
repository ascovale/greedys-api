# Script per modernizzare .gitlab-ci.yml

$filePath = ".gitlab-ci.yml"
$content = Get-Content -Path $filePath -Raw

# Sostituisci il publish job completo
$oldPublish = @"
publish:
  image: openjdk:17-slim
  stage: publish
  before_script:
    - echo "Setup ambiente per generazione Dart..."
    - apt-get update && apt-get install -y curl git unzip jq
    
    # SCARICA JAR STANDALONE E TEMPLATES DALLA VERSIONE 1.0.0-SNAPSHOT
"@

$newPublish = @"
publish:
  image: openjdk:17-slim
  stage: publish
  before_script:
    - echo "Setup ambiente per generazione Dart..."
    - apt-get update && apt-get install -y curl git
    
    # Scarica OpenAPI Generator standard
"@

$content = $content -replace [regex]::Escape($oldPublish), $newPublish

# Salva il file
Set-Content -Path $filePath -Value $content

Write-Host "File aggiornato!" -ForegroundColor Green
