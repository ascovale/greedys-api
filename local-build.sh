#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

echo "Building the Docker image..."
docker build -t greedys_api_spring-app .

echo "Removing the existing service..."
docker service rm greedys_api_spring-app || echo "Service not found, skipping removal."

echo "Deploying the stack..."
docker stack deploy -c docker-compose.yml greedys_api
