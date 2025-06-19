#!/bin/bash
# Script per creare i segreti Docker Swarm
# Usage: ./generate_secrets.sh

set -e

# Lista dei segreti da creare
declare -a secrets=(
  "db_password"
  "email_password"
)

echo "--- Creazione segreti Docker Swarm ---"
for secret in "${secrets[@]}"; do
  if docker secret ls --format '{{.Name}}' | grep -q "^$secret$"; then
    echo "[!] Il segreto '$secret' esiste già. Vuoi sovrascriverlo? (s/N)"
    read -r overwrite
    if [[ ! "$overwrite" =~ ^[sS]$ ]]; then
      echo "-> Skipping $secret"
      continue
    fi
    docker secret rm "$secret"
  fi

# Genera una password random di 32 caratteri
value=$(openssl rand -base64 32)
echo "Password generata per '$secret': $value"
echo -n "$value" | docker secret create "$secret" -
echo "-> Segreto '$secret' creato."
continue
done

# Scegli ambiente: localhost o api.greedys.it
DOMAIN="$1"
while [[ -z "$DOMAIN" ]]; do
  echo "Scegli per quale dominio generare il keystore:"
  select opt in "api.greedys.it" "localhost"; do
    case $REPLY in
      1) DOMAIN="api.greedys.it"; break 2;;
      2) DOMAIN="localhost"; break 2;;
      *) echo "Scelta non valida";;
    esac
  done
done

DNAME="CN=$DOMAIN, OU=IT, O=Greedys, L=Rome, ST=Rome, C=IT"

# Rimuovo i segreti esistenti (se non esistono, ignoro l'errore)
echo "Rimuovo i segreti esistenti..."
docker secret rm keystore 2>/dev/null || true
docker secret rm keystore_password 2>/dev/null || true

# Genero una nuova password casuale
echo "Generazione della nuova password..."
KEYSTORE_PASSWORD=$(openssl rand -hex 12)
echo "Nuova password generata."

echo "$KEYSTORE_PASSWORD" | docker secret create keystore_password -

# Genero il nuovo certificato keystore in /dev/shm per non scrivere su disco
KEYSTORE_TMP="/dev/shm/keystore.p12"


# Ensure the keystore file is removed before generation
if [ -f "$KEYSTORE_TMP" ]; then
    rm -f "$KEYSTORE_TMP"
fi

keytool -v -genkeypair \
    -alias "api.greedys.it" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -keystore "$KEYSTORE_TMP" \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -dname "$DNAME"

# Verify the keystore file
if [ ! -f "$KEYSTORE_TMP" ]; then
    echo "Errore: il file keystore non è stato generato correttamente."
    exit 1
fi

# Crea i nuovi segreti Docker
echo "Creazione del segreto 'keystore'..."
docker secret create keystore "$KEYSTORE_TMP"

# Pulizia dei file temporanei
rm "$KEYSTORE_TMP"

# rimuovo i segreti esistenti (se non esistono, ignoro l'errore)
docker secret rm service_account 2>/dev/null || true
docker secret create service_account ./secured/greedy-69de3-968988eeefce.json 

echo "--- Fine creazione segreti ---"