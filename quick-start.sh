#!/bin/bash

# Script per avvio veloce con profili
set -e

echo "🚀 Greedys API - Quick Start with Profiles"
echo "==========================================="

# Menu di selezione
echo "Seleziona modalità di avvio:"
echo "1) 🐌 Full (produzione/testing completo) - ~5-10 min startup"
echo "2) ⚡ Minimal only - ~15-30 sec startup"
echo "3) 🎯 Minimal + Google APIs - ~30-45 sec startup"
echo "4) 🔥 Minimal + Google + Firebase - ~45-60 sec startup"
echo "5) 💪 Minimal + Google + MySQL - per testing DB"
echo "6) 🛠️  Custom (scegli profili)"
echo ""
read -p "Scegli opzione (1-6): " choice

case $choice in
    1)
        echo "🐌 Avvio FULL (comportamento attuale)..."
        cd greedys_api
        mvn spring-boot:run -DskipTests
        ;;
    2)
        echo "⚡ Avvio MINIMAL..."
        cd greedys_api
        export SPRING_PROFILES_ACTIVE=dev-minimal
        mvn spring-boot:run -Pminimal -DskipTests
        ;;
    3)
        echo "🎯 Avvio MINIMAL + GOOGLE..."
        cd greedys_api
        export SPRING_PROFILES_ACTIVE=dev-minimal,google
        mvn spring-boot:run -Pminimal,google -DskipTests
        ;;
    4)
        echo "🔥 Avvio MINIMAL + GOOGLE + FIREBASE..."
        cd greedys_api
        export SPRING_PROFILES_ACTIVE=dev-minimal,google,firebase
        mvn spring-boot:run -Pminimal,google,firebase -DskipTests
        ;;
    5)
        echo "💪 Avvio MINIMAL + GOOGLE + MYSQL..."
        cd greedys_api
        export SPRING_PROFILES_ACTIVE=dev-minimal,google,mysql
        mvn spring-boot:run -Pminimal,google,mysql -DskipTests
        ;;
    6)
        echo "Profili disponibili: minimal, google, firebase, twilio, security, mail, mysql, monitoring, docs, testing"
        read -p "Inserisci profili Maven (separati da virgola): " maven_profiles
        read -p "Inserisci profili Spring (separati da virgola): " spring_profiles
        echo "🛠️  Avvio CUSTOM: Maven=[$maven_profiles] Spring=[$spring_profiles]..."
        cd greedys_api
        export SPRING_PROFILES_ACTIVE="$spring_profiles"
        mvn spring-boot:run -P"$maven_profiles" -DskipTests
        ;;
    *)
        echo "❌ Opzione non valida"
        exit 1
        ;;
esac

echo ""
echo "✅ Applicazione disponibile su:"
echo "   🌐 HTTP: http://localhost:8080"
echo "   📚 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   📄 OpenAPI JSON: http://localhost:8080/v3/api-docs"
echo "   📊 H2 Console: http://localhost:8080/h2-console (se usa H2)"
