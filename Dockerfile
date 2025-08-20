# Fase di Build con Maven - BACKWARD COMPATIBLE con profili opzionali
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app/greedys_api

# Installa Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Build arguments OPZIONALI (se vuoti = comportamento attuale IDENTICO)
ARG MAVEN_PROFILES=""
ARG SKIP_TESTS=true

# Prima copia SOLO pom.xml per scaricare le dipendenze
COPY ./greedys_api/pom.xml /app/greedys_api/pom.xml

# Scarica le dipendenze - MANTIENE logica originale se MAVEN_PROFILES è vuoto
RUN if [ -z "$MAVEN_PROFILES" ]; then \
        echo "🔄 Using DEFAULT behavior - downloading ALL dependencies"; \
        mvn dependency:go-offline -B; \
    else \
        echo "🎯 Using custom profiles: $MAVEN_PROFILES"; \
        mvn dependency:go-offline -P"$MAVEN_PROFILES" -B; \
    fi

# SOLO DOPO copia tutto il resto del codice sorgente
COPY ./greedys_api/src /app/greedys_api/src

# Build del progetto - MANTIENE logica originale se MAVEN_PROFILES è vuoto
RUN if [ -z "$MAVEN_PROFILES" ]; then \
        echo "🔄 Building with DEFAULT configuration (full profile)"; \
        mvn clean install -DskipTests="$SKIP_TESTS"; \
    else \
        echo "🎯 Building with custom profiles: $MAVEN_PROFILES"; \
        mvn clean install -P"$MAVEN_PROFILES" -DskipTests="$SKIP_TESTS"; \
    fi

# Fase di Runtime con Distroless (ZERO vulnerabilità)
FROM gcr.io/distroless/java17-debian12

# Copia del JAR costruito dalla fase di build
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Esposizione della porta dell'applicazione (HTTP per Traefik)
EXPOSE 8080

# Configurazione dell'entrypoint per avviare l'applicazione
ENTRYPOINT ["java", "-jar", "/app.jar"]
