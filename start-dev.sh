#!/bin/bash

echo "🚀 Avvio Greedys API - Modalità Sviluppo"
echo "================================================"
echo "💾 Database: H2 in memoria (~50MB RAM)"
echo "🌐 Server: http://localhost:8080"
echo "🗄️ H2 Console: http://localhost:8080/h2-console"
echo "📖 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "================================================"
echo "🔗 JDBC URL: jdbc:h2:mem:greedys_dev"
echo "👤 Username: sa (password vuota)"
echo "================================================"
echo

cd greedys_api

# Check if dependencies are already downloaded
if [ ! -d "target/dependency" ]; then
    echo "📦 Download dipendenze (prima volta)..."
    mvn dependency:resolve
fi

echo "🔧 Compilazione rapida..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Errore durante la compilazione!"
    exit 1
fi

echo
echo "🚀 Avvio con profilo dev (H2 database)..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Xmx256m" -q
