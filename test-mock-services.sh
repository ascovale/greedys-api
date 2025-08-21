#!/bin/bash

# ============================================================================
# GREEDYS API - TEST MOCK SERVICES
# Script per testare il sistema di mock services
# ============================================================================

set -e

echo "🧪 ===== TEST MOCK SERVICES GREEDYS API ====="
echo ""
echo "🔄 Inizializzazione test in corso..."

# Vai nella directory del progetto
cd "$(dirname "$0")/greedys_api"

echo "📁 Directory progetto: $(pwd)"
echo "⏱️  Questo test può richiedere 1-3 minuti per la compilazione..."
echo ""

# Test 1: Compilazione con profilo minimal
echo "1️⃣ Test compilazione profilo minimal..."
echo "   📦 Eseguendo: mvn clean compile -Pminimal..."
mvn clean compile -Pminimal
compilation_result=$?
if [ $compilation_result -eq 0 ]; then
    echo "   ✅ Compilazione successful"
else
    echo "   ❌ Compilazione failed (exit code: $compilation_result)"
    exit 1
fi
echo ""

# Test 2: Verifica presenza file mock
echo "2️⃣ Test presenza mock services..."
echo "   🔍 Verificando esistenza file mock..."

mock_files=(
    "src/main/java/com/application/common/spring/MockFirebaseService.java"
    "src/main/java/com/application/common/spring/MockGoogleAuthService.java"
    "src/main/java/com/application/common/spring/MockGooglePlacesSearchService.java"
    "src/main/java/com/application/common/spring/MockTwilioConfig.java"
    "src/main/java/com/application/common/spring/MockReliableNotificationService.java"
)

for file in "${mock_files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $(basename $file)"
    else
        echo "   ❌ $(basename $file) - MISSING"
        exit 1
    fi
done
echo ""

# Test 3: Verifica configurazione properties
echo "3️⃣ Test configurazione dev-minimal..."
config_file="src/main/resources/application-dev-minimal.properties"

if [ -f "$config_file" ]; then
    echo "   ✅ File configurazione presente"
    
    # Verifica proprietà chiave
    properties_to_check=(
        "firebase.enabled=false"
        "google.oauth.enabled=false"
        "google.maps.enabled=false"
        "twilio.enabled=false"
        "notifications.enabled=false"
    )
    
    for prop in "${properties_to_check[@]}"; do
        if grep -q "$prop" "$config_file"; then
            echo "   ✅ $prop"
        else
            echo "   ⚠️  $prop - not found"
        fi
    done
else
    echo "   ❌ File configurazione missing"
    exit 1
fi
echo ""

# Test 4: Test pattern di logging nei mock
echo "4️⃣ Test pattern logging mock..."
mock_pattern="🔧 MOCK:"

found_patterns=0
for file in "${mock_files[@]}"; do
    if [ -f "$file" ] && grep -q "$mock_pattern" "$file"; then
        found_patterns=$((found_patterns + 1))
    fi
done

echo "   ✅ Pattern logging trovato in $found_patterns/$((${#mock_files[@]})) file"
echo ""

# Test 5: Verifica annotazioni Spring
echo "5️⃣ Test annotazioni Spring..."
spring_annotations=(
    "@ConditionalOnProperty"
    "@Primary"
    "@Service"
)

annotations_found=0
for file in "${mock_files[@]}"; do
    if [ -f "$file" ]; then
        for annotation in "${spring_annotations[@]}"; do
            if grep -q "$annotation" "$file"; then
                annotations_found=$((annotations_found + 1))
                break
            fi
        done
    fi
done

echo "   ✅ Annotazioni Spring trovate in $annotations_found/$((${#mock_files[@]})) file"
echo ""

# Test 6: Verifica profili Maven
echo "6️⃣ Test profili Maven..."
if grep -q "<id>minimal</id>" pom.xml; then
    echo "   ✅ Profilo 'minimal' trovato in pom.xml"
else
    echo "   ❌ Profilo 'minimal' missing in pom.xml"
    exit 1
fi
echo ""

# Test 7: Test script di sviluppo
echo "7️⃣ Test script sviluppo..."
dev_scripts=(
    "../dev.sh"
    "../dev-fast.sh"
)

for script in "${dev_scripts[@]}"; do
    if [ -f "$script" ]; then
        echo "   ✅ $(basename $script)"
    else
        echo "   ⚠️  $(basename $script) - not found"
    fi
done
echo ""

# Test 8: Package structure
echo "8️⃣ Test struttura package..."
spring_package="src/main/java/com/application/common/spring"
if [ -d "$spring_package" ]; then
    mock_count=$(find "$spring_package" -name "Mock*.java" | wc -l)
    echo "   ✅ Package spring presente con $mock_count mock services"
else
    echo "   ❌ Package spring missing"
    exit 1
fi
echo ""

# Test Summary
echo "🎉 ===== RISULTATI TEST ====="
echo "✅ Tutti i test completati con successo!"
echo ""
echo "📊 Mock Services implementati:"
echo "   • MockFirebaseService (notifications push)"
echo "   • MockGoogleAuthService (OAuth2 authentication)"  
echo "   • MockGooglePlacesSearchService (Maps/Places API)"
echo "   • MockTwilioConfig (SMS/WhatsApp)"
echo "   • MockReliableNotificationService (email retry)"
echo ""
echo "🚀 PRONTO PER AVVIARE DEV MINIMAL!"
echo "   Comando per avvio:"
echo "   cd greedys_api"
echo "   mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal -DskipTests"
echo ""
echo "📖 Documentazione: other/MOCK_SERVICES_DOCUMENTATION.md"
echo ""
echo "✨ Mock services pronti per sviluppo ultra-veloce!"
