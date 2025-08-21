#!/bin/bash

# Script per avvio sviluppo veloce
set -e

# Colori
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo "🚀 GREEDYS API - Sviluppo Ultra-Veloce"
echo "======================================"
echo ""
print_info "📊 Configurazione MINIMAL:"
echo "   🔧 Profilo Maven: minimal"
echo "   🌱 Profilo Spring: dev-minimal"
echo "   💾 Database: H2 in memoria"
echo "   🔒 Security: JWT dev mode"
echo "   🌐 Porta: 8080 (HTTP)"
echo "   🔧 Servizi esterni: MOCK"
echo "   ⏱️  Startup atteso: 30-60 secondi"
echo ""

# Vai alla directory del progetto
cd "$(dirname "$0")/greedys_api"

print_info "🔄 Avvio applicazione..."
print_warning "Servizi MOCK attivi: Google APIs, Firebase, Twilio"
echo ""

# Avvio con configurazione ottimizzata
mvn spring-boot:run \
    -Pminimal \
    -Dspring-boot.run.profiles=dev-minimal \
    -DskipTests=true \
    -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -XX:+UseG1GC"

echo ""
print_success "✅ Applicazione terminata"
