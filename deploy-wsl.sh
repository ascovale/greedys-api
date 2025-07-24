#!/bin/bash

# Deploy script for Greedys API
# This script builds the Docker image, deploys the stack, and follows the logs

set -e  # Exit on any error

echo "🚀 Starting Greedys API deployment..."

# Step 1: Build Docker image
echo "📦 Building Docker image..."
docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest .

if [ $? -eq 0 ]; then
    echo "✅ Docker image built successfully"
else
    echo "❌ Failed to build Docker image"
    exit 1
fi

# Step 2: Deploy stack
echo "🔄 Deploying Docker stack..."
docker stack deploy -c docker-compose.yml greedys_api

if [ $? -eq 0 ]; then
    echo "✅ Docker stack deployed successfully"
else
    echo "❌ Failed to deploy Docker stack"
    exit 1
fi

# Step 3: Wait a moment for services to start
echo "⏳ Waiting for services to start..."
sleep 5

# Step 4: Follow logs
echo "📋 Following service logs (Press Ctrl+C to stop)..."
docker service logs greedys_api_spring-app -f
