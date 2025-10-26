#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

echo "Pulling the latest Docker image..."
docker pull registry.gitlab.com/greedysgroup/greedys_api:latest

echo "Checking and creating traefik directory..."
mkdir -p traefik

echo "Creating acme.json if it doesn't exist..."
if [ ! -f traefik/acme.json ]; then
  touch traefik/acme.json
  chmod 600 traefik/acme.json
fi

echo "Checking and creating Flutter app directory..."
mkdir -p ~/restaurant-app
chmod 755 ~/restaurant-app

echo "Removing the existing stack..."
docker stack rm greedys_api || echo "Stack not found, skipping removal."

echo "Waiting for stack removal to complete..."
sleep 10

echo "Deploying the stack with Traefik + HTTPS + Flutter App..."
docker stack deploy -c docker-compose.yml greedys_api

echo "Stack deployed successfully!"
echo "Traefik dashboard: http://traefik.greedys.it:8080/dashboard"
echo "API HTTPS: https://api.greedys.it/swagger-ui.html"
echo "Flutter App: https://app.greedys.it"
