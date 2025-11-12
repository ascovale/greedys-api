#!/bin/bash
# Script di inizializzazione RabbitMQ
# Legge i secrets e configura users/vhosts

set -e

# Leggi i secrets
RABBITMQ_USER=$(cat /run/secrets/rabbitmq_user 2>/dev/null || echo "guest")
RABBITMQ_PASS=$(cat /run/secrets/rabbitmq_password 2>/dev/null || echo "guest")

echo "🔐 Configurazione RabbitMQ in corso..."
echo "   User: $RABBITMQ_USER"

# Attendi che RabbitMQ sia online
echo "⏳ Attesa startup RabbitMQ..."
rabbitmq-diagnostics ping || sleep 5

# Crea/aggiorna l'utente
echo "👤 Configurazione utente RabbitMQ..."
rabbitmqctl add_user "$RABBITMQ_USER" "$RABBITMQ_PASS" 2>/dev/null || \
rabbitmqctl change_password "$RABBITMQ_USER" "$RABBITMQ_PASS"

# Imposta permessi
rabbitmqctl set_permissions -p "/greedys" "$RABBITMQ_USER" ".*" ".*" ".*"

# Crea vhost se non esiste
echo "🏠 Creazione vhost /greedys..."
rabbitmqctl add_vhost "/greedys" 2>/dev/null || echo "vhost /greedys esiste già"

echo "✅ RabbitMQ configurato!"
