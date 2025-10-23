$filePath = "C:\Users\ascol\Projects\greedys_api\.gitlab-ci.yml"

# Leggi il file
$lines = @(Get-Content $filePath)

# Trova l'indice della riga con "publish:"
$publishIndex = $lines.FindIndex(0, { $args[0] -eq "publish:" })
Write-Host "Found 'publish:' at line $($publishIndex + 1)"

# Usa soltanto le righe prima di publish
$beforePublish = $lines[0..$($publishIndex - 1)] -join "`n"

# Crea il nuovo publish job
$newPublish = @'
publish:
  image: openjdk:17-slim
  stage: publish
  before_script:
    - echo "Setup ambiente per generazione Dart..."
    - apt-get update && apt-get install -y curl git
    - echo "Scarico OpenAPI Generator v${OPENAPI_GENERATOR_VERSION}..."
    - mkdir -p tools
    - curl -sSL "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${OPENAPI_GENERATOR_VERSION}/openapi-generator-cli-${OPENAPI_GENERATOR_VERSION}.jar" -o "tools/openapi-generator.jar"
    - export GENERATOR_JAR="$(pwd)/tools/openapi-generator.jar"
    - echo "echo \$PAT" > /tmp/git-askpass.sh
    - chmod +x /tmp/git-askpass.sh
    - export GIT_ASKPASS=/tmp/git-askpass.sh
    - git clone https://gitlab.com/GreedysGroup/greedys_api_dart_lib.git
    - cd greedys_api_dart_lib
    - git config user.email "ci-bot@greedys.com"
    - git config user.name "Greedys CI Bot"

  script:
    - echo "Attendo che l'API sia pronta..."
    - sleep 10
    - |
      for i in {1..30}; do
        echo "Tentativo $i/30: Testing ${API_DOCS_BASE}/restaurant-api"
        if curl -f --max-time 15 "${API_DOCS_BASE}/restaurant-api" >/dev/null 2>&1; then
          echo "API docs pronte!"
          break
        fi
        sleep 10
        if [ $i -eq 30 ]; then
          echo "TIMEOUT: API non risponde"
          exit 1
        fi
      done
    - echo "Pulisco cartelle client API precedenti..."
    - rm -rf restaurant_api admin_api customer_api
    - |
      for GROUP in $API_GROUPS; do
        SPEC_FILE="./openapi-${GROUP}.json"
        OUTPUT_DIR="./${GROUP}_api"
        echo ""
        echo ">>> Generazione client $GROUP <<<"
        echo "Scarico spec da ${API_DOCS_BASE}/${GROUP}-api"
        if curl -sSfk -o "$SPEC_FILE" "${API_DOCS_BASE}/${GROUP}-api"; then
          echo "Spec scaricata: $(wc -c < "$SPEC_FILE") bytes"
        else
          echo "ERRORE: Impossibile scaricare spec per $GROUP"
          exit 1
        fi
        echo "Genero client $GROUP con OpenAPI Generator ${OPENAPI_GENERATOR_VERSION}..."
        java -jar $GENERATOR_JAR generate \
          -g dart \
          -i $SPEC_FILE \
          -o $OUTPUT_DIR \
          --skip-validate-spec \
          -p pubName=${GROUP}_api \
          -p pubVersion=1.0.0 \
          -p pubDescription="Greedys ${GROUP^} API client" \
          -p pubHomepage=https://greedys.it 2>&1 | grep -v "^WARN" || true
        if [ ! -d "$OUTPUT_DIR" ] || [ ! -f "$OUTPUT_DIR/pubspec.yaml" ]; then
          echo "ERRORE: Generazione client $GROUP fallita"
          exit 1
        fi
        echo "Client $GROUP generato con successo"
        PUBSPEC="$OUTPUT_DIR/pubspec.yaml"
        if [ -f "$PUBSPEC" ] && ! grep -q "common:" "$PUBSPEC"; then
          echo "Aggiungo dipendenza common a pubspec.yaml..."
          sed -i '/^dependencies:/a\  common:\n    path: ..\/common' "$PUBSPEC"
          echo "Dipendenza common aggiunta"
        fi
        rm "$SPEC_FILE"
        echo "Client $GROUP completato!"
      done
    - echo ""
    - echo "Commit e push delle modifiche..."
    - git add .
    - |
      if git diff-index --quiet HEAD; then
        echo "Nessuna modifica da pushare"
      else
        git commit -m "Genera Dart clients v${OPENAPI_GENERATOR_VERSION}: $API_GROUPS"
        git push origin main
        echo "Client v${OPENAPI_GENERATOR_VERSION} pushati con successo!"
      fi

  only:
    - main
'@

# Scrivi il nuovo file
$newContent = $beforePublish + "`n" + $newPublish
Set-Content $filePath -Value $newContent

Write-Host "File aggiornato con successo!" -ForegroundColor Green
