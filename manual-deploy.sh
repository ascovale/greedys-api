#!/bin/bash
###############################################################################
# Deploy HTTPS Manuale - Greedys API
# Esegui questo script come ROOT sul server
# bash /tmp/deploy-greedys.sh
###############################################################################

set -e

DOMAIN="api.greedys.it"
EMAIL="beeinggreedy@gmail.com"
DEPLOY_DIR="/opt/greedys_api"

echo "🚀 DEPLOYMENT HTTPS GREEDYS API"
echo "================================"
echo ""

# 1. Crea directory
echo "1️⃣  Creando directory..."
mkdir -p $DEPLOY_DIR/{traefik,secrets}

# 2. Genera secrets
echo "2️⃣  Generando secrets..."
openssl rand -base64 32 > $DEPLOY_DIR/secrets/db_password.txt
openssl rand -base64 64 > $DEPLOY_DIR/secrets/jwt_secret.txt
chmod 600 $DEPLOY_DIR/secrets/*

# 3. Crea acme.json
echo "3️⃣  Creando acme.json..."
touch $DEPLOY_DIR/traefik/acme.json
chmod 600 $DEPLOY_DIR/traefik/acme.json

# 4. Imposta permessi
echo "4️⃣  Impostando permessi..."
chown -R deployer:deployer $DEPLOY_DIR

# 5. Salva i secrets
echo ""
echo "✅ Setup completato!"
echo ""
echo "Database password:"
cat $DEPLOY_DIR/secrets/db_password.txt
echo ""
echo "JWT secret:"
cat $DEPLOY_DIR/secrets/jwt_secret.txt
echo ""
echo "✨ Adesso trasferisci i file di configurazione:"
echo ""
echo "scp docker-compose.prod-https.yml deployer@46.101.209.92:$DEPLOY_DIR/"
echo "scp traefik/traefik-https.yml deployer@46.101.209.92:$DEPLOY_DIR/traefik/traefik.yml"
echo "scp traefik/dynamic-https.yml deployer@46.101.209.92:$DEPLOY_DIR/traefik/dynamic.yml"
