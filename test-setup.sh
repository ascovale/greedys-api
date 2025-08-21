#!/bin/bash

# Script di test per verificare la configurazione
set -e

echo "🧪 TEST CONFIGURAZIONE GREEDYS API"
echo "=================================="

cd greedys_api

# Test 1: Verifica profili Maven
echo "1️⃣ Test profili Maven disponibili..."
if mvn help:all-profiles -q | grep -q "minimal"; then
    echo "✅ Profilo 'minimal' trovato"
else
    echo "❌ Profilo 'minimal' non trovato"
fi

# Test 2: Compilazione profilo full (default)
echo "2️⃣ Test compilazione profilo full (default)..."
if mvn clean compile -DskipTests -q; then
    echo "✅ Compilazione full OK"
else
    echo "❌ Compilazione full FALLITA"
    exit 1
fi

# Test 3: Compilazione profilo minimal
echo "3️⃣ Test compilazione profilo minimal..."
if mvn clean compile -Pminimal -DskipTests -q; then
    echo "✅ Compilazione minimal OK"
else
    echo "❌ Compilazione minimal FALLITA"
    exit 1
fi

# Test 4: Verifica file application-dev-minimal.properties
echo "4️⃣ Verifica file configurazione dev-minimal..."
if [ -f "src/main/resources/application-dev-minimal.properties" ]; then
    echo "✅ File application-dev-minimal.properties trovato"
else
    echo "❌ File application-dev-minimal.properties mancante"
fi

# Test 5: Verifica dimensioni dependency tree
echo "5️⃣ Confronto dipendenze..."
FULL_DEPS=$(mvn dependency:tree -Pfull 2>/dev/null | grep -c "├─\|└─" || echo "0")
MINIMAL_DEPS=$(mvn dependency:tree -Pminimal 2>/dev/null | grep -c "├─\|└─" || echo "0")

echo "   📦 Dipendenze profilo full: ~$FULL_DEPS"
echo "   📦 Dipendenze profilo minimal: ~$MINIMAL_DEPS"

if [ "$MINIMAL_DEPS" -lt "$FULL_DEPS" ]; then
    echo "✅ Profilo minimal ha meno dipendenze"
else
    echo "⚠️  Profilo minimal ha troppe dipendenze"
fi

cd ..

# Test 6: Verifica script
echo "6️⃣ Verifica script..."
if [ -x "dev.sh" ] && [ -x "dev-fast.sh" ]; then
    echo "✅ Script eseguibili configurati"
else
    echo "❌ Script non eseguibili"
fi

echo ""
echo "🎉 TEST COMPLETATI!"
echo ""
echo "📚 Come usare:"
echo "   ./dev.sh           - Menu selezione modalità"
echo "   ./dev-fast.sh      - Avvio veloce diretto"
echo "   ./start.sh         - Docker produzione (invariato)"
echo ""
echo "🌐 URL modalità sviluppo:"
echo "   API: http://localhost:8080"
echo "   Swagger: http://localhost:8080/swagger-ui.html"
echo "   H2 Console: http://localhost:8080/h2-console"
