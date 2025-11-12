#!/bin/bash

# ===== CREATE RABBITMQ SECRETS ON DOCKER SWARM =====
# Script per creare i secrets RabbitMQ sul server

SERVER_IP="46.101.209.92"
USER="deployer"
SSH_KEY="/home/user/.ssh/deploy_key"

echo "🚀 Creating RabbitMQ Secrets on Docker Swarm..."
echo "Server: $SERVER_IP"
echo ""

# Genera password randomica per RabbitMQ
RABBIT_PASSWORD=$(openssl rand -base64 32)

echo "📝 RabbitMQ Credentials:"
echo "   Username: greedys"
echo "   Password: $RABBIT_PASSWORD"
echo ""

# Esegui i comandi SSH per creare i secrets
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$USER@$SERVER_IP" << EOF

echo "🔐 Creating Docker Swarm Secrets..."

# Funzione per creare secret se non esiste
create_secret() {
    SECRET_NAME=\$1
    SECRET_VALUE=\$2
    
    if docker secret inspect "\$SECRET_NAME" >/dev/null 2>&1; then
        echo "✅ Secret '\$SECRET_NAME' already exists"
    else
        echo "\$SECRET_VALUE" | docker secret create "\$SECRET_NAME" -
        echo "✅ Created secret: \$SECRET_NAME"
    fi
}

# Crea i secrets
create_secret "rabbitmq_user" "greedys"
create_secret "rabbitmq_password" "$RABBIT_PASSWORD"

echo ""
echo "✅ RabbitMQ Secrets Created!"
echo ""
echo "📋 Current Secrets:"
docker secret ls

EOF

echo ""
echo "✅ DONE! Secrets created on server."
echo ""
echo "📝 Keep this password safe:"
echo "   $RABBIT_PASSWORD"
echo ""

