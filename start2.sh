#!/bin/bash

# start2.sh - Avvio Docker Swarm con Traefik e SSL
# Utilizza i secrets esistenti e configura SSL tramite Traefik

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funzione per stampare messaggi colorati
print_message() {
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

# Banner
echo "========================================="
echo "  Greedys API - Docker Swarm (No Traefik)"
echo "========================================="

# Verifica se Docker Swarm è inizializzato
print_message "Verifico stato Docker Swarm..."
if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
    print_warning "Docker Swarm non è attivo. Inizializzo..."
    docker swarm init --advertise-addr $(hostname -I | awk '{print $1}') || {
        print_error "Errore durante l'inizializzazione di Docker Swarm"
        exit 1
    }
    print_success "Docker Swarm inizializzato!"
else
    print_success "Docker Swarm già attivo"
fi

# Verifica e crea le reti necessarie
print_message "Creo le reti Docker..."
docker network ls --format "table {{.Name}}" | grep -q "app-network" || {
    print_message "Creazione rete app-network..."
    docker network create --driver overlay --attachable app-network
}

# Verifica esistenza secrets
print_message "Verifico esistenza secrets..."
REQUIRED_SECRETS=("db_password" "email_password" "service_account" "jwt_secret")
MISSING_SECRETS=()

for secret in "${REQUIRED_SECRETS[@]}"; do
    if ! docker secret ls --format "{{.Name}}" | grep -q "^${secret}$"; then
        MISSING_SECRETS+=("$secret")
    fi
done

# Se mancano secrets, richiamiamo generate_secrets.sh
if [ ${#MISSING_SECRETS[@]} -ne 0 ]; then
    print_warning "Alcuni secrets mancano: ${MISSING_SECRETS[*]}"
    print_message "Eseguo ./generate_secrets.sh per creare i secrets..."
    
    if [ ! -f "./generate_secrets.sh" ]; then
        print_error "File generate_secrets.sh non trovato!"
        exit 1
    fi
    
    chmod +x ./generate_secrets.sh
    ./generate_secrets.sh || {
        print_error "Errore durante la creazione dei secrets"
        exit 1
    }
fi

print_success "Tutti i secrets sono disponibili"

# Verifica configurazione Traefik
print_message "Verifico configurazione Traefik..."
if [ ! -d "traefik" ]; then
    print_warning "Cartella traefik non trovata. La creo..."
    mkdir -p traefik
fi

if [ ! -f "traefik/dynamic.yml" ]; then
    print_message "Creo configurazione dinamica Traefik..."
    cat > traefik/dynamic.yml << 'EOF'
# Configurazione dinamica Traefik
http:
  middlewares:
    # Security headers
    security-headers:
      headers:
        customRequestHeaders:
          X-Forwarded-Proto: "https"
          X-Forwarded-Port: "443"
        customResponseHeaders:
          X-Frame-Options: "SAMEORIGIN"
          X-Content-Type-Options: "nosniff"
          X-XSS-Protection: "1; mode=block"
          Strict-Transport-Security: "max-age=31536000; includeSubDomains"
        
    # Rate limiting
    rate-limit:
      rateLimit:
        burst: 100
        average: 50

# TLS Options
tls:
  options:
    default:
      minVersion: "VersionTLS12"
      cipherSuites:
        - "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
        - "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305"
        - "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
EOF
fi

# Build dell'immagine Docker (sempre necessario per Swarm)
print_message "Inizio build dell'immagine Docker..."
print_warning "Questo processo potrebbe richiedere alcuni minuti..."

if docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest .; then
    print_success "Build dell'immagine completata con successo!"
else
    print_error "Errore durante la build dell'immagine Docker"
    exit 1
fi

# Stop di eventuali stack esistenti
print_message "Arresto stack esistenti..."
docker stack ls --format "{{.Name}}" | grep -q "greedys_api" && {
    print_message "Arresto stack greedys_api..."
    docker stack rm greedys_api
    print_message "Attendo la rimozione completa..."
    sleep 10
}

# Deploy dello stack
print_message "Deploy dello stack Docker Swarm con Traefik SSL..."
docker stack deploy -c docker-compose.local-traefik.yml greedys_api || {
    print_error "Errore durante il deploy dello stack"
    exit 1
}

print_success "Deploy dello stack completato!"

# Scala a 0 l'applicazione Spring Boot inizialmente
print_message "Scala l'applicazione Spring Boot a 0 replicas..."
docker service scale greedys_api_greedys-api=0

# Attesa per database e Traefik
print_message "Attendo che database e Traefik siano pronti..."
sleep 8

# Verifica che Traefik sia avviato
print_message "Verifico stato Traefik..."
while ! docker service ls --format "{{.Name}} {{.Replicas}}" | grep "greedys_api_traefik" | grep -q "1/1"; do
    print_message "Traefik ancora in avvio..."
    sleep 3
done
print_success "Traefik pronto!"

# Mostra logs Traefik
print_message "Logs Traefik (ultimi 15 righe):"
docker service logs --tail 15 greedys_api_traefik

# Verifica che il database sia avviato
print_message "Attendo 10 secondi per il database..."
sleep 10
print_success "Database dovrebbe essere pronto!"

# Scala l'applicazione Spring Boot a 1 replica
print_message "Avvio dell'applicazione Spring Boot (scala a 1 replica)..."
docker service scale greedys_api_greedys-api=1

# Attesa breve per l'avvio dell'applicazione
print_message "Attendo 2 secondi e mostro i logs..."
sleep 2

# Mostra stato finale dei servizi
print_message "Stato finale dei servizi:"
docker stack services greedys_api

print_success "Setup completato!"

# Mostra logs dell'applicazione Spring Boot in tempo reale
print_message "Logs Spring Boot in tempo reale (premi Ctrl+C per uscire):"
docker service logs -f greedys_api_greedys-api
echo ""
echo "========================================="
echo "  INFORMAZIONI DI ACCESSO"
echo "========================================="
echo "🌐 API Endpoint: https://api.greedys.com"
echo "🔒 SSL: Gestito da Traefik (Let's Encrypt)"
echo "📊 Traefik Dashboard: Non abilitato (sicurezza)"
echo ""
echo "📋 Comandi utili:"
echo "  docker stack services greedys_api    # Stato servizi"
echo "  docker service logs greedys_api_spring-app  # Logs applicazione"
echo "  docker service logs greedys_api_traefik     # Logs Traefik"
echo "  docker stack rm greedys_api          # Rimuovi stack"
echo ""
echo "⚠️  Assicurati che:"
echo "  - Il dominio api.greedys.com punti a questo server"
echo "  - Le porte 80 e 443 siano accessibili"
echo "  - I certificati SSL verranno generati automaticamente"
echo "========================================="
