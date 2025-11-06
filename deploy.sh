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
sleep 5

# Wait for network cleanup with retry mechanism
echo "Waiting for network cleanup..."
max_attempts=6
attempt=1

while [ $attempt -le $max_attempts ]; do
  if ! docker network ls | grep -q "greedys_api_app-network"; then
    echo "Network cleanup complete"
    break
  fi
  
  echo "Network still exists, waiting... (attempt $attempt/$max_attempts)"
  sleep 5
  attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
  echo "Warning: Network cleanup timed out, proceeding anyway"
  # Force cleanup if needed
  docker network rm greedys_api_app-network || echo "Network already removed or in use"
fi

echo "Additional safety wait..."
sleep 5

echo "Deploying the stack with Traefik + HTTPS + Flutter App..."
docker stack deploy -c docker-compose.yml greedys_api

echo "Stack deployed successfully!"
echo "Traefik dashboard: http://traefik.greedys.it:8080/dashboard"
echo "API HTTPS: https://api.greedys.it/swagger-ui.html"
echo "Flutter App: https://app.greedys.it"
