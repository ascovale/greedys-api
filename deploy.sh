#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

echo "Pulling the latest Docker image..."
docker pull registry.gitlab.com/greedysgroup/greedys_api:latest

echo "Removing the existing stack..."
docker stack rm greedys_api || echo "Stack not found, skipping removal."

echo "Waiting for stack removal to complete..."
sleep 10

echo "Deploying the stack with Traefik + HTTPS..."
docker stack deploy -c docker-compose.yml greedys_api

echo "Stack deployed successfully!"
echo "Traefik dashboard: http://traefik.greedys.it:8080/dashboard"
echo "API HTTPS: https://api.greedys.it/swagger-ui.html"
