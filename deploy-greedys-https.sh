#!/bin/bash
###############################################################################
# HTTPS Deploy Script for greedys_api - PERSONALIZZATO
# Server: api.greedys.it
# Questo script automatizza il deploy del progetto Greedys su HTTPS
###############################################################################

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

###############################################################################
# CONFIGURAZIONE - PERSONALIZZATA PER GREEDYS
###############################################################################

# Dati dal progetto
DOMAIN="api.greedys.it"
EMAIL="admin@greedys.it"
DEPLOY_DIR="/opt/greedys_api"
DEPLOY_USER="deployer"  # Utente dal .gitlab-ci.yml

print_header "🚀 DEPLOYMENT SETUP PERSONALIZZATO PER GREEDYS"

print_info "Dominio: $DOMAIN"
print_info "Email Let's Encrypt: $EMAIL"
print_info "Deploy user: $DEPLOY_USER"
print_info "Deploy dir: $DEPLOY_DIR"

# Chiedi l'IP del server
print_warning "DEVO SAPERE L'IP DEL SERVER!"
read -p "📍 Inserisci SERVER_IP (es: 123.45.67.89): " SERVER_IP

if [ -z "$SERVER_IP" ]; then
    print_error "SERVER_IP è richiesto!"
    exit 1
fi

print_info "Server IP: $SERVER_IP"

###############################################################################
# VERIFICA ACCESSO
###############################################################################

print_header "VERIFICA ACCESSO AL SERVER"

print_info "Verificando connessione SSH a deployer@$SERVER_IP..."

if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=accept-new $DEPLOY_USER@$SERVER_IP "echo OK" &> /dev/null; then
    print_success "SSH connesso!"
else
    print_error "Impossibile connettersi a $DEPLOY_USER@$SERVER_IP"
    print_warning "Verifica:"
    echo "  1. L'IP è corretto?"
    echo "  2. SSH è abilitato sul server?"
    echo "  3. La chiave SSH è configurata?"
    echo "  4. L'utente 'deployer' esiste?"
    exit 1
fi

###############################################################################
# PREPARAZIONE SERVER
###############################################################################

print_header "PREPARAZIONE SERVER"

print_info "Creando directory e secrets sul server..."

ssh $DEPLOY_USER@$SERVER_IP << 'EOF'
    set -e
    
    echo "Creando directory..."
    mkdir -p $DEPLOY_DIR/{traefik,secrets}
    
    echo "Generando secrets..."
    openssl rand -base64 32 | tee $DEPLOY_DIR/secrets/db_password.txt > /dev/null
    chmod 600 $DEPLOY_DIR/secrets/db_password.txt
    
    openssl rand -base64 64 | tee $DEPLOY_DIR/secrets/jwt_secret.txt > /dev/null
    chmod 600 $DEPLOY_DIR/secrets/jwt_secret.txt
    
    echo "Creando acme.json per certificati..."
    touch $DEPLOY_DIR/traefik/acme.json
    chmod 600 $DEPLOY_DIR/traefik/acme.json
    
    echo "Setup completato!"
EOF

print_success "Server preparato"

###############################################################################
# LETTURA SECRETS
###############################################################################

print_header "SECRETS GENERATI"

DB_PASSWORD=$(ssh $DEPLOY_USER@$SERVER_IP "cat $DEPLOY_DIR/secrets/db_password.txt")
JWT_SECRET=$(ssh $DEPLOY_USER@$SERVER_IP "cat $DEPLOY_DIR/secrets/jwt_secret.txt")

print_info "Database password: ${DB_PASSWORD:0:8}... (salvato in secrets/db_password.txt)"
print_info "JWT secret: salvato in secrets/jwt_secret.txt"

###############################################################################
# TRANSFER FILE
###############################################################################

print_header "TRANSFER FILE DI CONFIGURAZIONE"

FILES=(
    "docker-compose.prod-https.yml"
    "traefik/traefik-https.yml"
    "traefik/dynamic-https.yml"
)

for file in "${FILES[@]}"; do
    print_info "Trasferendo $file..."
    
    # Crea directory remota
    REMOTE_DIR=$(dirname "$file")
    if [ "$REMOTE_DIR" != "." ]; then
        ssh $DEPLOY_USER@$SERVER_IP "mkdir -p $DEPLOY_DIR/$REMOTE_DIR"
    fi
    
    # Transfer file
    scp -q "$file" "$DEPLOY_USER@$SERVER_IP:$DEPLOY_DIR/$file"
    
    print_success "$file trasferito"
done

###############################################################################
# RINOMINA FILE
###############################################################################

print_header "CONFIGURAZIONE FILE"

print_info "Rinominando file Traefik..."

ssh $DEPLOY_USER@$SERVER_IP << RENAME
    cd $DEPLOY_DIR
    
    # Rinomina per overwrite dei file originali
    mv traefik/traefik-https.yml traefik/traefik.yml 2>/dev/null || cp traefik/traefik-https.yml traefik/traefik.yml
    mv traefik/dynamic-https.yml traefik/dynamic.yml 2>/dev/null || cp traefik/dynamic-https.yml traefik/dynamic.yml
    
    echo "File rinominati"
RENAME

print_success "File configurati"

###############################################################################
# VERIFICA
###############################################################################

print_header "VERIFICA PRE-DEPLOY"

print_info "Verificando file sul server..."

ssh $DEPLOY_USER@$SERVER_IP << VERIFY
if [ -f "$DEPLOY_DIR/docker-compose.prod-https.yml" ]; then
    echo "✓ docker-compose.prod-https.yml"
else
    echo "✗ docker-compose.prod-https.yml MANCANTE"
    exit 1
fi

if [ -f "$DEPLOY_DIR/traefik/traefik.yml" ]; then
    echo "✓ traefik/traefik.yml"
else
    echo "✗ traefik/traefik.yml MANCANTE"
    exit 1
fi

if [ -f "$DEPLOY_DIR/traefik/dynamic.yml" ]; then
    echo "✓ traefik/dynamic.yml"
else
    echo "✗ traefik/dynamic.yml MANCANTE"
    exit 1
fi

if [ -f "$DEPLOY_DIR/traefik/acme.json" ]; then
    echo "✓ traefik/acme.json"
else
    echo "✗ traefik/acme.json MANCANTE"
    exit 1
fi

echo "✓ Tutti i file sono presenti"
VERIFY

print_success "Tutti i file verificati"

###############################################################################
# DNS VERIFICATION
###############################################################################

print_header "VERIFICA DNS"

print_info "Verificando se $DOMAIN punta a $SERVER_IP..."

DNS_IP=$(nslookup $DOMAIN 8.8.8.8 2>/dev/null | grep -A1 "Name:" | grep "Address:" | awk '{print $2}' || echo "non risolto")

if [ "$SERVER_IP" == "$DNS_IP" ]; then
    print_success "DNS OK - $DOMAIN → $SERVER_IP"
else
    print_warning "DNS potrebbe non essere corretto"
    echo "Server IP: $SERVER_IP"
    echo "Domain IP: $DNS_IP"
    echo ""
    print_info "Se il DNS non è ancora propagato, aspetta 5-30 minuti e riprova"
fi

###############################################################################
# DOCKER LAUNCH
###############################################################################

print_header "LANCIO DOCKER COMPOSE"

print_info "Avviando stack HTTPS sul server..."

ssh $DEPLOY_USER@$SERVER_IP << DEPLOY
    cd $DEPLOY_DIR
    
    echo "Verificando docker-compose..."
    docker-compose --version
    
    echo "Validando configurazione..."
    docker-compose -f docker-compose.prod-https.yml config > /dev/null && echo "✓ Configurazione valida"
    
    echo "Avviando container..."
    docker-compose -f docker-compose.prod-https.yml up -d
    
    echo "Aspettando che i container si avviino..."
    sleep 5
    
    echo "✓ Docker lanciato!"
DEPLOY

print_success "Docker Compose avviato"

###############################################################################
# RIEPILOGO
###############################################################################

print_header "✨ DEPLOYMENT COMPLETATO"

cat << EOF

${GREEN}Setup HTTPS completato per greedys_api!${NC}

🖥️  Server: $SERVER_IP
🌐 Dominio: $DOMAIN
📧 Email: $EMAIL
📂 Deploy dir: $DEPLOY_DIR
👤 Deploy user: $DEPLOY_USER

${YELLOW}PROSSIMI STEP:${NC}

1️⃣  Monitora i log di Traefik (5-10 minuti per il certificato):
   ssh $DEPLOY_USER@$SERVER_IP
   cd $DEPLOY_DIR
   docker-compose -f docker-compose.prod-https.yml logs -f traefik

2️⃣  Aspetta fino a che non vedi:
   "Certificate obtained"
   "Certificate renewed"

3️⃣  Una volta pronto, testa HTTPS:
   curl -v https://$DOMAIN/swagger-ui.html
   
   Oppure nel browser:
   https://$DOMAIN/swagger-ui.html

4️⃣  Verifica il certificato:
   echo | openssl s_client -servername $DOMAIN -connect $DOMAIN:443 2>/dev/null | openssl x509 -noout -dates

${BLUE}────────────────────────────────────────${NC}

${RED}⚠️  IMPORTANTE - BACKUP:${NC}
Salva questi file in un posto sicuro:

ssh $DEPLOY_USER@$SERVER_IP "cat $DEPLOY_DIR/secrets/db_password.txt"
ssh $DEPLOY_USER@$SERVER_IP "cat $DEPLOY_DIR/secrets/jwt_secret.txt"

Se perdi questi file, dovrai ricreare il database e i JWT!

${GREEN}Buon deploy! 🚀${NC}

EOF

###############################################################################
# SALVA CONFIGURAZIONE
###############################################################################

# Crea file di configurazione locale per riferimento futuro
cat > deployment-config.env << EOF
# Deployment Configuration - Greedys API HTTPS
DOMAIN=$DOMAIN
EMAIL=$EMAIL
SERVER_IP=$SERVER_IP
DEPLOY_USER=$DEPLOY_USER
DEPLOY_DIR=$DEPLOY_DIR
TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

print_success "Configurazione salvata in deployment-config.env"

print_header "✅ SETUP COMPLETATO!"
print_info "Esegui il monitoring come indicato sopra"

