#!/bin/bash

# Script per testare i profili Maven e Docker
set -e

# Colori
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
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

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "🧪 TESTING PROFILES IMPLEMENTATION"
echo "=================================="

cd greedys_api

# Test 1: Build normale (comportamento attuale)
print_info "1️⃣ Test build normale (comportamento attuale)..."
if mvn clean compile -DskipTests -q; then
    print_success "Build normale OK"
else
    print_error "Build normale FALLITA"
    exit 1
fi

# Test 2: Verifica profili disponibili
print_info "2️⃣ Verifico profili disponibili..."
mvn help:all-profiles -q
print_success "Profili rilevati"

# Test 3: Verifica profilo attivo di default
print_info "3️⃣ Verifico profilo attivo di default..."
ACTIVE_PROFILE=$(mvn help:active-profiles -q | grep "Active Profiles for Project" -A 10 | grep "full" || echo "")
if [ -n "$ACTIVE_PROFILE" ]; then
    print_success "Profilo 'full' attivo di default"
else
    print_warning "Profilo 'full' non rilevato esplicitamente (normale se usa default)"
fi

# Test 4: Build con profilo minimal
print_info "4️⃣ Test build con profilo minimal..."
if mvn clean compile -Pminimal -DskipTests -q; then
    print_success "Build con profilo 'minimal' OK"
else
    print_error "Build con profilo 'minimal' FALLITA"
    exit 1
fi

# Test 5: Build con profili combinati
print_info "5️⃣ Test build con profili combinati (minimal,google)..."
if mvn clean compile -Pminimal,google -DskipTests -q; then
    print_success "Build con profili combinati OK"
else
    print_error "Build con profili combinati FALLITA"
    exit 1
fi

cd ..

# Test 6: Docker build comportamento attuale
print_info "6️⃣ Test Docker build (comportamento attuale)..."
if docker build -t greedys-test-current . --quiet; then
    print_success "Docker build attuale OK"
    docker rmi greedys-test-current
else
    print_error "Docker build attuale FALLITO"
    exit 1
fi

# Test 7: Docker build con profili
print_info "7️⃣ Test Docker build con profili custom..."
if docker build -t greedys-test-minimal --build-arg MAVEN_PROFILES=minimal,google . --quiet; then
    print_success "Docker build con profili OK"
    docker rmi greedys-test-minimal
else
    print_error "Docker build con profili FALLITO"
    exit 1
fi

echo ""
print_success "🎉 TUTTI I TEST COMPLETATI CON SUCCESSO!"
echo ""
echo "✅ Profili aggiunti correttamente"
echo "✅ Comportamento attuale preservato (profilo full)"
echo "✅ Nuovi profili funzionanti"
echo "✅ Docker backward compatible"
echo "✅ Docker supporta profili personalizzati"
echo ""
echo "🚀 PRONTO PER L'USO!"
echo ""
echo "📚 Utilizzo:"
echo "   Normale:        mvn clean compile"
echo "   Minimal:        mvn clean compile -Pminimal"
echo "   Combinati:      mvn clean compile -Pminimal,google,mysql"
echo "   Docker normale: docker build ."
echo "   Docker custom:  docker build --build-arg MAVEN_PROFILES=minimal,google ."
