#!/bin/bash

# Script di avvio rapido per sviluppo
# Ottimizzato per ridurre i tempi di compilazione

echo "🚀 Avvio rapido modalità sviluppo..."

cd greedys_api

# Verifica se è necessaria una compilazione completa
if [ ! -d "target/classes" ] || [ ! -f "target/classes/application.properties" ]; then
    echo "📦 Prima compilazione necessaria..."
    mvn clean compile -DskipTests -q
else
    echo "♻️  Usando cache esistente, compilazione incrementale..."
    mvn compile -DskipTests -q
fi

echo "🔥 Avvio con hot reload attivato..."

# Avvia con:
# - Profilo dev 
# - Skip test
# - Hot reload abilitato
# - JVM ottimizzata per sviluppo
# - Riduzione log non essenziali
mvn spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -DskipTests \
    -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true" \
    -Dspring-boot.run.fork=true \
    -q
