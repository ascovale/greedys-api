#!/bin/bash

# Script per scegliere modalità di sviluppo
set -e

echo "🚀 GREEDYS API - Selezione Modalità"
echo "==================================="
echo ""
echo "Scegli modalità di sviluppo:"
echo ""
echo "1) 🐌 PRODUZIONE/DOCKER (start.sh) - Stack completo con secrets"
echo "2) 🔧 SVILUPPO COMPLETO (dev) - Tutte le funzionalità per testing"
echo "3) ⚡ SVILUPPO VELOCE (minimal) - Core + mock services"
echo "4) 📊 CONFRONTA configurazioni"
echo ""
read -p "Inserisci scelta (1-4): " choice

case $choice in
    1)
        echo ""
        echo "🐌 MODALITÀ PRODUZIONE/DOCKER"
        echo "=============================="
        echo "✅ Stack completo con Docker Swarm"
        echo "✅ Tutti i secrets configurati"
        echo "✅ MySQL database"
        echo "✅ HTTPS su porta 5050"
        echo "🕐 Tempo startup: 5-10 minuti"
        echo ""
        read -p "Confermi avvio con ./start.sh? (y/N): " confirm
        if [[ "$confirm" =~ ^[Yy]$ ]]; then
            echo "🚀 Avvio stack produzione..."
            ./start.sh
        else
            echo "❌ Operazione annullata"
        fi
        ;;
    2)
        echo ""
        echo "🔧 MODALITÀ SVILUPPO COMPLETO"
        echo "============================="
        echo "✅ Google APIs configurate"
        echo "✅ Twilio attivo"
        echo "✅ Database H2 locale"
        echo "✅ Logging dettagliato"
        echo "🕐 Tempo startup: 2-3 minuti"
        echo ""
        cd greedys_api
        echo "🚀 Avvio sviluppo completo..."
        mvn spring-boot:run \
            -Pminimal,google,twilio \
            -Dspring-boot.run.profiles=dev \
            -DskipTests=true
        ;;
    3)
        echo ""
        echo "⚡ MODALITÀ SVILUPPO VELOCE"
        echo "=========================="
        echo "🔧 Solo core essenziale"
        echo "🔧 Mock services (Google, Twilio, Firebase)"
        echo "🔧 H2 in memoria"
        echo "🔧 Logging minimale"
        echo "🕐 Tempo startup: 30-60 secondi"
        echo ""
        echo "🚀 Avvio sviluppo veloce..."
        ./dev-fast.sh
        ;;
    4)
        echo ""
        echo "📊 CONFRONTO CONFIGURAZIONI"
        echo "==========================="
        echo ""
        echo "🐌 PRODUZIONE (start.sh + docker):"
        echo "   🔑 Secrets: keystore, db_password, service_account"
        echo "   🗄️ Database: MySQL con volume persistente"
        echo "   🌐 Porte: 5050:8443 (HTTPS)"
        echo "   📦 Dipendenze: Tutte (53 deps)"
        echo "   🕐 Build + Startup: 8-15 minuti"
        echo ""
        echo "🔧 SVILUPPO COMPLETO (dev):"
        echo "   🔑 API Keys: Configurate per testing"
        echo "   🗄️ Database: H2 locale"
        echo "   🌐 Porte: 8080 (HTTP)"
        echo "   📦 Dipendenze: Profilo minimal + addons"
        echo "   🕐 Startup: 2-3 minuti"
        echo ""
        echo "⚡ SVILUPPO VELOCE (dev-minimal):"
        echo "   🔧 API Keys: Mock services"
        echo "   🗄️ Database: H2 memoria"
        echo "   🌐 Porte: 8080 (HTTP)"
        echo "   📦 Dipendenze: Solo core (15 deps)"
        echo "   🕐 Startup: 30-60 secondi"
        echo ""
        echo "🌐 URLs disponibili in modalità sviluppo:"
        echo "   API: http://localhost:8080"
        echo "   Swagger: http://localhost:8080/swagger-ui.html"
        echo "   H2 Console: http://localhost:8080/h2-console"
        echo "   Health: http://localhost:8080/actuator/health"
        ;;
    *)
        echo "❌ Scelta non valida"
        exit 1
        ;;
esac
