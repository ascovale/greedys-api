#!/bin/bash

# Script per build/run Docker con profili
set -e

echo "🐳 Greedys API - Docker with Profiles"
echo "======================================"

# Menu di selezione
echo "Seleziona modalità Docker:"
echo "1) 🐌 Full (comportamento attuale) - tutte le dipendenze"
echo "2) ⚡ Build Minimal - immagine leggera"
echo "3) 🎯 Build Minimal + Google"
echo "4) 🔥 Build Custom - scegli profili"
echo "5) 🚀 Run existing image"
echo ""
read -p "Scegli opzione (1-5): " choice

case $choice in
    1)
        echo "🐌 Building FULL Docker image..."
        docker build -t greedys-api:full .
        echo "✅ Immagine creata: greedys-api:full"
        echo "🚀 Per avviare: docker run -p 8080:8080 greedys-api:full"
        ;;
    2)
        echo "⚡ Building MINIMAL Docker image..."
        docker build -t greedys-api:minimal \
            --build-arg MAVEN_PROFILES=minimal \
            .
        echo "✅ Immagine creata: greedys-api:minimal"
        echo "🚀 Per avviare: docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev-minimal greedys-api:minimal"
        ;;
    3)
        echo "🎯 Building MINIMAL + GOOGLE Docker image..."
        docker build -t greedys-api:minimal-google \
            --build-arg MAVEN_PROFILES=minimal,google \
            .
        echo "✅ Immagine creata: greedys-api:minimal-google"
        echo "🚀 Per avviare: docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev-minimal,google greedys-api:minimal-google"
        ;;
    4)
        echo "Profili disponibili: minimal, google, firebase, twilio, security, mail, mysql, monitoring, docs, testing"
        read -p "Inserisci profili Maven (separati da virgola): " maven_profiles
        read -p "Inserisci tag immagine (es: custom): " image_tag
        echo "🛠️  Building CUSTOM Docker image..."
        docker build -t greedys-api:$image_tag \
            --build-arg MAVEN_PROFILES="$maven_profiles" \
            .
        echo "✅ Immagine creata: greedys-api:$image_tag"
        echo "🚀 Per avviare: docker run -p 8080:8080 greedys-api:$image_tag"
        ;;
    5)
        echo "Immagini disponibili:"
        docker images greedys-api --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
        echo ""
        read -p "Inserisci tag immagine da avviare: " image_tag
        echo "🚀 Avvio greedys-api:$image_tag..."
        docker run -p 8080:8080 \
            -e SPRING_PROFILES_ACTIVE=dev-minimal \
            --name greedys-dev \
            --rm \
            greedys-api:$image_tag
        ;;
    *)
        echo "❌ Opzione non valida"
        exit 1
        ;;
esac

if [ "$choice" != "5" ]; then
    echo ""
    echo "📊 Confronto dimensioni immagini:"
    docker images greedys-api --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    echo ""
    echo "💡 Suggerimenti:"
    echo "   - Minimal: ~30-50% più piccola, startup 5-10x più veloce"
    echo "   - Full: Completa, ideale per produzione"
    echo "   - Custom: Scegli solo le dipendenze che ti servono"
fi
