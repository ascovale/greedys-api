# Fase di Build con Maven
FROM maven:latest AS builder

WORKDIR /app/greedys_api

# Prima copia solo pom.xml per scaricare le dipendenze
COPY ./greedys_api/pom.xml /app/greedys_api/pom.xml

# Scarica le dipendenze (questa layer sarà cached se pom.xml non cambia)
RUN mvn dependency:go-offline -B

# Ora copia tutto il resto del codice sorgente
COPY ./greedys_api/ /app/greedys_api

# Build del progetto (saltando i test temporaneamente)
RUN mvn clean install -DskipTests

# Fase di Runtime con JDK Slim
FROM openjdk:19-jdk-slim

# Installazione di netcat
RUN apt-get update && apt-get install -y netcat

# Copia del JAR costruito dalla fase di build
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Esposizione della porta dell'applicazione
EXPOSE 8443
# Configurazione dell'entrypoint per avviare l'applicazione dopo aver atteso la connessione al database
ENTRYPOINT ["java", "-jar", "/app.jar"]
