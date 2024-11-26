#!/bin/bash

# Parametri
IMAGE_NAME="registry.gitlab.com/psychoorange/greedys_api/spring-app:latest"
STACK_NAME="greedys_api"
COMPOSE_FILE="docker-compose.yml"

# 2. Ottieni il digest dell'immagine
DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' $IMAGE_NAME)

if [ -z "$DIGEST" ]; then
  echo "Errore: impossibile ottenere il digest per l'immagine $IMAGE_NAME"
  exit 1
fi

# 3. Esegui il deploy dello stack specificando l'immagine con il digest e passando le credenziali del registro
docker stack deploy --with-registry-auth -c <(sed "s|registry.gitlab.com/psychoorange/greedys_api/spring-app:latest|$DIGEST|g" $COMPOSE_FILE) $STACK_NAME

# Controllo del successo del comando di deploy
if [ $? -eq 0 ]; then
  echo "Docker stack deploy completato con successo usando l'immagine: $DIGEST"
else
  echo "Errore durante il deployment dello stack Docker"
  exit 1
fi
