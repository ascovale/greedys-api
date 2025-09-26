#!/bin/bash

set -e  # Exit on any error

echo "🚀 Test generazione client Dart (Pipeline Replica)"
echo "================================================="

# Pulisci precedenti test
rm -rf ../generated-clients

# Usa Docker per replicare l'ambiente pipeline
docker run --rm \
  -v "$(pwd):/workspace" \
  -v "$(pwd)/../generated-clients:/generated-clients" \
  -w /workspace \
  ubuntu:20.04 \
  bash -c "
    set -e
    
    # Aggiorna e installa dipendenze
    apt-get update
    apt-get install -y openjdk-11-jdk maven curl wget unzip
    
    echo '✅ Java:' \$(java -version 2>&1 | head -n1)
    echo '✅ Maven:' \$(mvn -version 2>&1 | head -n1)
    
    # Variabili ambiente come nella pipeline
    export WRAPPERS_ENDPOINT=\"http://api.greedys.it:8080/api/internal/response-wrapper-catalog\"
    export API_GROUPS=\"restaurant admin customer\"
    
    # Crea directory per le dipendenze e JAR
    mkdir -p tools
    cd tools
    
    # Scarica OpenAPI Generator CLI JAR (come nella pipeline)
    echo '📦 Downloading OpenAPI Generator CLI...'
    wget -q https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/7.8.0/openapi-generator-cli-7.8.0.jar
    
    cd ..
    
    echo '📦 Building custom generator...'
    mvn clean package -DskipTests
    
    echo '📋 Verifica build Maven:'
    echo 'JAR principale:' \$(ls -la target/*.jar || echo 'MANCANTE!')
    echo 'Directory dipendenze:' \$(ls -la target/lib/ || echo 'MANCANTE!')
    echo 'Dipendenze trovate:' \$(ls target/lib/*.jar 2>/dev/null | wc -l || echo '0')
    
    # Verifica che il build sia riuscito
    if [ ! -f \"target/custom-dart-codegen-1.0.0-SNAPSHOT.jar\" ]; then
        echo \"❌ ERRORE: JAR custom generator non trovato!\"
        exit 1
    fi
    
    if [ ! -d \"target/lib\" ] || [ \$(ls target/lib/*.jar 2>/dev/null | wc -l) -eq 0 ]; then
        echo \"❌ ERRORE: Dipendenze non trovate in target/lib/\"
        echo \"Il plugin maven-dependency-plugin potrebbe non aver funzionato\"
        exit 1
    fi
    
    # Copia custom JAR E le sue dipendenze in tools (come nella pipeline)
    cp target/custom-dart-codegen-1.0.0-SNAPSHOT.jar tools/
    cp target/lib/*.jar tools/
    
    echo \"✅ JAR copiati con successo in tools/\"
    
    # Configura JAR path con tutte le dipendenze
    export NPM_OPENAPI_JAR=\"tools/openapi-generator-cli-7.8.0.jar\"
    export CUSTOM_JAR=\"tools/custom-dart-codegen-1.0.0-SNAPSHOT.jar\"
    
    # Crea classpath completo con tutte le dipendenze
    CLASSPATH=\"\$NPM_OPENAPI_JAR:\$CUSTOM_JAR\"
    for jar in tools/*.jar; do
        if [[ \"\$jar\" != \"tools/openapi-generator-cli-7.8.0.jar\" && \"\$jar\" != \"tools/custom-dart-codegen-1.0.0-SNAPSHOT.jar\" ]]; then
            CLASSPATH=\"\$CLASSPATH:\$jar\"
        fi
    done
    export FULL_CLASSPATH=\"\$CLASSPATH\"
    
    echo 'JAR configurati:'
    echo 'OpenAPI JAR:' \"\$NPM_OPENAPI_JAR\"
    echo 'Custom JAR:' \"\$CUSTOM_JAR\"
    echo 'Classpath completo:' \"\$FULL_CLASSPATH\"
    ls -la tools/
    
    # Test connettività endpoints e scarica le vere API
    echo \"\"
    echo \"🔍 Download specifiche OpenAPI reali:\"
    for GROUP in \$API_GROUPS; do
        echo \"- Scarico OpenAPI spec per \$GROUP...\"
        if curl -sSfk -o \"openapi-\${GROUP}.json\" \"http://api.greedys.it:8080/v3/api-docs/\${GROUP}-api\" 2>/dev/null; then
            echo \"✅ Spec \$GROUP scaricata da http://api.greedys.it:8080/v3/api-docs/\${GROUP}-api\"
            echo \"Dimensione: \$(wc -c < \"openapi-\${GROUP}.json\") bytes\"
            echo \"Endpoints trovati: \$(grep -o '\"paths\"' \"openapi-\${GROUP}.json\" | wc -l)\"
        else
            echo \"❌ Impossibile scaricare OpenAPI spec per \$GROUP\"
            echo \"Creo spec minimale di fallback...\"
            # Crea spec minimale solo in caso di errore
            cat > \"openapi-\${GROUP}.json\" << 'TESTAPI'
{
  \"openapi\": \"3.0.3\",
  \"info\": {
    \"title\": \"Test API\",
    \"version\": \"1.0.0\"
  },
  \"paths\": {
    \"/test\": {
      \"get\": {
        \"summary\": \"Test endpoint\",
        \"responses\": {
          \"200\": {
            \"description\": \"Success\",
            \"content\": {
              \"application/json\": {
                \"schema\": {
                  \"\$ref\": \"#/components/schemas/ResponseWrapper\"
                }
              }
            }
          }
        }
      }
    }
  },
  \"components\": {
    \"schemas\": {
      \"ResponseWrapper\": {
        \"type\": \"object\",
        \"properties\": {
          \"success\": {
            \"type\": \"boolean\"
          },
          \"message\": {
            \"type\": \"string\"
          },
          \"data\": {
            \"type\": \"object\"
          }
        }
      },
      \"User\": {
        \"type\": \"object\",
        \"properties\": {
          \"id\": {
            \"type\": \"integer\"
          },
          \"name\": {
            \"type\": \"string\"
          }
        }
      }
    }
  }
}
TESTAPI
        fi
    done
    
    # Testa ogni gruppo (replica esatta della pipeline)
    for GROUP in \$API_GROUPS; do
        SPEC_FILE=\"./openapi-\${GROUP}.json\"
        OUTPUT_DIR=\"/generated-clients/\${GROUP}_api\"
        
        echo \"\"
        echo \">>> Generazione client per \$GROUP <<<\"
        
        # Scarica response wrappers specifico per gruppo (come nella pipeline)
        echo \"- Scarico catalogo response wrappers per gruppo \$GROUP...\"
        if curl -sSfk -o \"response-wrappers-\${GROUP}.json\" \"\$WRAPPERS_ENDPOINT/\$GROUP\" 2>/dev/null; then
            echo \"✅ Catalogo \$GROUP scaricato da \$WRAPPERS_ENDPOINT/\$GROUP\"
            echo \"Dimensione: \$(wc -c < \"response-wrappers-\${GROUP}.json\") bytes\"
        else
            echo \"❌ Impossibile scaricare da \$WRAPPERS_ENDPOINT/\$GROUP\"
            # Fallback: usa file locale generico se esiste
            if [ -f \"response-wrappers.json\" ]; then
                cp \"response-wrappers.json\" \"response-wrappers-\${GROUP}.json\"
                echo \"⚠️ Uso fallback da file locale response-wrappers.json\"
            else
                echo \"❌ Nessun catalogo disponibile per \$GROUP\"
                # Crea un catalogo minimale di emergenza
                echo '{\"message\": \"No wrappers catalog\"}' > \"response-wrappers-\${GROUP}.json\"
            fi
        fi
        
        # GENERA CON CUSTOM GENERATOR (esatta replica della pipeline)
        echo \"- 🎯 Genero con CUSTOM generator in \$OUTPUT_DIR\"
        
        # Usa il classpath completo con tutte le dipendenze
        java -cp \"\$FULL_CLASSPATH\" org.openapitools.codegen.OpenAPIGenerator generate \\
          -g org.openapitools.custom.CustomDartClientCodegen \\
          -i \"\$SPEC_FILE\" \\
          -o \"\$OUTPUT_DIR\" \\
          -t \"templates\" \\
          --ignore-file-override \".openapi-generator-ignore\" \\
          --skip-validate-spec \\
          -p pubName=\${GROUP}_api \\
          -p pubVersion=1.0.0 \\
          -p pubDescription=\"Greedys \${GROUP^} API client\" \\
          -p pubHomepage=https://greedys.it \\
          --config=\"response-wrappers-\${GROUP}.json\" || echo \"❌ Generazione fallita per \$GROUP\"
        
        # Pulizia come nella pipeline
        echo \"- Post-processing per \$GROUP...\"
        
        # Rimuovi doc ResponseWrapper indesiderati
        [ -d \"\$OUTPUT_DIR/doc\" ] && find \"\$OUTPUT_DIR/doc\" -name \"ResponseWrapper*.md\" ! -name \"ResponseWrapperErrorDetails.md\" -delete 2>/dev/null || true
        
        # Rimuovi test ResponseWrapper indesiderati  
        [ -d \"\$OUTPUT_DIR/test\" ] && find \"\$OUTPUT_DIR/test\" -name \"response_wrapper*.dart\" ! -name \"response_wrapper_error_details_test.dart\" -delete 2>/dev/null || true
        
        # Pulisci file temporanei
        rm -f \"openapi-\${GROUP}.json\" \"response-wrappers-\${GROUP}.json\"
        
        echo \"✅ Client \$GROUP processato\"
    done
    
    echo \"\"
    echo \"📂 Struttura generata:\"
    find /generated-clients -type f | head -20
    
    echo \"\"
    echo \"🔍 Verifica risultati:\"
    
    for GROUP in \$API_GROUPS; do
        echo \"\"
        echo \"--- Gruppo \$GROUP ---\"
        
        if [ -f \"/generated-clients/\${GROUP}_api/lib/openapi.dart\" ]; then
            echo \"   ✅ openapi.dart generato\"
        else
            echo \"   ❌ openapi.dart mancante\"
        fi
        
        if [ ! -f \"/generated-clients/\${GROUP}_api/lib/src/model/response_wrapper.dart\" ]; then
            echo \"   ✅ ResponseWrapper.dart filtrato\"
        else
            echo \"   ❌ ResponseWrapper.dart NON filtrato\"
        fi
        
        if [ -f \"/generated-clients/\${GROUP}_api/lib/src/model/user.dart\" ]; then
            echo \"   ✅ Modello User generato\"
        else
            echo \"   ❌ Modello User mancante\"
        fi
        
        echo \"   📁 Struttura lib:\"
        find \"/generated-clients/\${GROUP}_api/lib\" -name \"*.dart\" 2>/dev/null | head -10 || echo \"   ❌ Nessun file dart trovato\"
    done
    
    echo \"\"
    echo \"🎉 Test completato!\"
"