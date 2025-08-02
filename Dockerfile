# Fase di Build con Maven
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app/greedys_api

# Installa Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Prima copia SOLO pom.xml per scaricare le dipendenze
COPY ./greedys_api/pom.xml /app/greedys_api/pom.xml

# Scarica le dipendenze (questa layer sarà cached se pom.xml non cambia)
RUN mvn dependency:go-offline -B

# SOLO DOPO copia tutto il resto del codice sorgente
COPY ./greedys_api/src /app/greedys_api/src

# Build del progetto (saltando i test temporaneamente)
RUN mvn clean install -DskipTests

# Fase di Runtime con Distroless (ZERO vulnerabilità)
FROM gcr.io/distroless/java17-debian12

# Copia del JAR costruito dalla fase di build
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Esposizione della porta dell'applicazione (HTTP per Traefik)
EXPOSE 8080

# Configurazione dell'entrypoint per avviare l'applicazione
ENTRYPOINT ["java", "-jar", "/app.jar"]
