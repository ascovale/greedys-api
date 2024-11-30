#!/bin/bash

# Parametri
IMAGE_NAME="registry.gitlab.com/psychoorange/greedys_api/spring-app:latest"
STACK_NAME="greedys_api"
COMPOSE_FILE="docker-compose.yml"

# Ottieni il digest
docker pull $IMAGE_NAME
DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' $IMAGE_NAME)

if [ -z "$DIGEST" ]; then
  echo "Errore: impossibile ottenere il digest per l'immagine $IMAGE_NAME"
  exit 1
fi

# Crea un file temporaneo con la modifica
TEMP_COMPOSE_FILE=$(mktemp)
sed "s|registry.gitlab.com/psychoorange/greedys_api/spring-app:latest|$DIGEST|g" $COMPOSE_FILE > $TEMP_COMPOSE_FILE

# Esegui il deploy
docker stack deploy --with-registry-auth -c $TEMP_COMPOSE_FILE $STACK_NAME

# Controllo del successo del comando di deploy
if [ $? -eq 0 ]; then
  echo "Docker stack deploy completato con successo usando l'immagine: $DIGEST"
else
  echo "Errore durante il deployment dello stack Docker"
  exit 1
fi

# Rimuovi il file temporaneo
rm $TEMP_COMPOSE_FILE
