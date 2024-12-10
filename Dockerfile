# Fase di Build con Maven
FROM maven:latest AS builder

WORKDIR /app/greedys_api

# Copia del codice sorgente nel container
COPY ./greedys_api/ /app/greedys_api

# Build del progetto
RUN mvn clean install

# Fase di Runtime con JDK Slim
FROM openjdk:19-jdk-slim

# Copia del JAR costruito dalla fase di build
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Esposizione della porta dell'applicazione
EXPOSE 8443

# Configurazione dell'entrypoint per avviare l'applicazione
ENTRYPOINT ["java", "-jar", "/app.jar"]
