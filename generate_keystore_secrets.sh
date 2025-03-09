#!/bin/bash
set -e

# Rimuovo i segreti esistenti (se non esistono, ignoro l'errore)
echo "Rimuovo i segreti esistenti..."
docker secret rm keystore 2>/dev/null || true
docker secret rm keystore_password 2>/dev/null || true

# Genero una nuova password casuale
echo "Generazione della nuova password..."
KEYSTORE_PASSWORD=$(openssl rand -hex 12)
echo "Nuova password generata."

# Salvo la password in un file temporaneo in /dev/shm (in memoria)
PASSWORD_TMP="/dev/shm/keystore_password.txt"
echo "$KEYSTORE_PASSWORD" > "$PASSWORD_TMP"

# Genero il nuovo certificato keystore in /dev/shm per non scrivere su disco
KEYSTORE_TMP="/dev/shm/keystore.p12"
# Personalizza il campo -dname secondo le tue esigenze
keytool -genkeypair \
    -alias api.greedys.it \
    -keyalg RSA \
    -keysize 2048 \
    -validity 365 \
    -keystore "$KEYSTORE_TMP" \
    -storetype PKCS12 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -dname "CN=api.greedys.it, OU=IT, O=Greedys, L=Rome, ST=Rome, C=IT"

# Crea i nuovi segreti Docker
echo "Creazione del segreto 'keystore'..."
docker secret create keystore "$KEYSTORE_TMP"
echo "Creazione del segreto 'keystore_password'..."
docker secret create keystore_password "$PASSWORD_TMP"

# Pulizia dei file temporanei
rm "$KEYSTORE_TMP" "$PASSWORD_TMP"

echo "Script completato con successo!"
