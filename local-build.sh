#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

# Controlla se è stata passata la flag -clean
if [[ "$1" == "-clean" ]]; then
    echo "Using 'mvn clean install' for the build..."
    MVN_COMMAND="mvn clean install"
else
    echo "Using 'mvn install' for the build..."
    MVN_COMMAND="mvn install"
fi

echo "Building the project with Maven..."
docker build --build-arg MVN_COMMAND="$MVN_COMMAND" -t registry.gitlab.com/psychoorange/greedys_api:latest .

echo "Removing the existing service..."
docker service rm greedys_api_spring-app || echo "Service not found, skipping removal."

echo "Deploying the stack..."
docker stack deploy -c docker-compose.yml greedys_api