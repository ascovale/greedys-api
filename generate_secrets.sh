#!/bin/bash
# Script per creare i segreti Docker Swarm
# Usage: ./generate_secrets.sh

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

echo "========================================="
echo "  Creazione Segreti Docker Swarm"
echo "========================================="

# Verifica che Docker Swarm sia attivo
if ! docker info --format '{{.Swarm.LocalNodeState}}' | grep -q "active"; then
    print_error "Docker Swarm non è attivo!"
    exit 1
fi

# Lista dei segreti da creare (generati random)
declare -a secrets=(
  "db_password"
  "email_password"
  "jwt_secret"
)

print_message "Creazione segreti base..."
for secret in "${secrets[@]}"; do
  if docker secret ls --format '{{.Name}}' | grep -q "^$secret$"; then
    print_warning "Il segreto '$secret' esiste già. Vuoi sovrascriverlo? (s/N)"
    read -r overwrite
    if [[ ! "$overwrite" =~ ^[sS]$ ]]; then
      print_message "Skipping $secret"
      continue
    fi
    docker secret rm "$secret"
    print_message "Segreto '$secret' rimosso"
  fi

  # Genera una password random di 32 caratteri
  value=$(openssl rand -base64 32)
  echo -n "$value" | docker secret create "$secret" -
  print_success "Segreto '$secret' creato"
done

# Gestione Google OAuth Client Secret (valore specifico)
print_message "Creazione google_oauth_client_secret..."
docker secret rm google_oauth_client_secret 2>/dev/null || true
echo -n "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" | docker secret create google_oauth_client_secret -
print_success "Segreto google_oauth_client_secret creato"

# Gestione service account
print_message "Creazione segreto service_account..."
docker secret rm service_account 2>/dev/null || true

if [ -f "./secured/greedy-69de3-968988eeefce.json" ]; then
    docker secret create service_account ./secured/greedy-69de3-968988eeefce.json
    print_success "Segreto service_account creato"
else
    print_warning "File service account non trovato in ./secured/greedy-69de3-968988eeefce.json"
fi

print_success "Tutti i segreti sono stati creati con successo!"

print_message "Lista segreti Docker:"
docker secret ls

echo "========================================="
echo "📝 Segreti creati:"
echo "   • db_password (generato)"
echo "   • email_password (generato)" 
echo "   • jwt_secret (generato)"
echo "   • google_oauth_client_secret (valore specifico)"
echo "   • service_account (da file)"
echo ""
echo "ℹ️  SSL/HTTPS è gestito da Traefik"
echo "========================================="