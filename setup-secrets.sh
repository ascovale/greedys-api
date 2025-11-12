#!/bin/bash

# ===== DOCKER SWARM SECRETS SETUP SCRIPT =====
# 
# Crea i Docker secrets UNA SOLA VOLTA sul server Swarm.
# Eseguire MANUALMENTE sul server manager, NON dalla pipeline!
#
# Uso:
#   1. SSH nel server: ssh deployer@YOUR_SERVER_IP
#   2. Esegui: bash setup-secrets.sh
#   3. Verifica: docker secret ls
#
# NOTA: Se un secret esiste già, non viene ricreato (safe)

set -e

echo "=========================================="
echo "🔐 Docker Swarm Secrets Setup"
echo "=========================================="
echo ""

# Function to create secret only if it doesn't exist
create_secret_if_not_exists() {
    SECRET_NAME=$1
    SECRET_FILE=$2
    SECRET_VALUE=$3
    
    if docker secret inspect "$SECRET_NAME" >/dev/null 2>&1; then
        echo "✅ Secret '$SECRET_NAME' already exists (skipped)"
        return 0
    fi
    
    if [ -n "$SECRET_FILE" ] && [ -f "$SECRET_FILE" ]; then
        # Se il file esiste, leggi da file
        echo "Creating secret from file: $SECRET_FILE"
        cat "$SECRET_FILE" | docker secret create "$SECRET_NAME" -
        echo "✅ Created secret '$SECRET_NAME' from file"
    elif [ -n "$SECRET_VALUE" ]; then
        # Altrimenti usa il valore diretto
        echo "$SECRET_VALUE" | docker secret create "$SECRET_NAME" -
        echo "✅ Created secret '$SECRET_NAME' with value"
    else
        echo "❌ ERROR: No value or file provided for $SECRET_NAME"
        return 1
    fi
}

echo "📝 Creating secrets..."
echo ""

# ===== DATABASE SECRETS =====
echo "1️⃣  Database Secrets:"
create_secret_if_not_exists \
    "db_password" \
    "" \
    "$(openssl rand -base64 32)"
echo ""

# ===== EMAIL SECRETS =====
echo "2️⃣  Email Secrets:"
echo "⚠️  ATTENTION: Change the email password!"
create_secret_if_not_exists \
    "email_password" \
    "" \
    "your_email_password_here"
echo ""

# ===== JWT SECRETS =====
echo "3️⃣  JWT Secrets:"
create_secret_if_not_exists \
    "jwt_secret" \
    "" \
    "$(openssl rand -base64 64)"
echo ""

# ===== SERVICE ACCOUNT (Firebase) =====
echo "4️⃣  Service Account Secrets (Firebase):"
echo "⚠️  If you have a service-account.json file:"
create_secret_if_not_exists \
    "service_account" \
    "./service-account.json" \
    "{}"
echo ""

# ===== RABBITMQ SECRETS (NEW) =====
echo "5️⃣  RabbitMQ Secrets:"
create_secret_if_not_exists \
    "rabbitmq_user" \
    "" \
    "greedys"
echo ""

create_secret_if_not_exists \
    "rabbitmq_password" \
    "" \
    "$(openssl rand -base64 32)"
echo ""

# ===== SUMMARY =====
echo "=========================================="
echo "✅ Secrets Setup Complete!"
echo "=========================================="
echo ""
echo "📋 Created/Verified Secrets:"
docker secret ls
echo ""
echo "📝 RabbitMQ Credentials:"
echo "   Username: greedys"
echo "   Password: (stored in 'rabbitmq_password' secret)"
echo ""
echo "📝 RabbitMQ Access:"
echo "   AMQP: rabbitmq:5672 (interno al Swarm)"
echo "   Management UI: https://rabbitmq.greedys.it (esterno via Traefik)"
echo "   Default VHost: /greedys"
echo ""
echo "⚠️  IMPORTANT MANUAL STEPS:"
echo "   1. Change 'email_password' if using SMTP"
echo "   2. Add service-account.json for Firebase if needed"
echo "   3. Update RabbitMQ password if needed:"
echo "      docker secret rm rabbitmq_password"
echo "      echo 'your_new_password' | docker secret create rabbitmq_password -"
echo ""
echo "✅ Ready to deploy! Run: bash deploy.sh"
echo ""

