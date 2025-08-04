# Fase di Build con Maven
FROM maven:latest AS builder

WORKDIR /app/greedys_api

# Copia solo i file di configurazione Maven per sfruttare la cache
COPY ./greedys_api/pom.xml ./
RUN mvn dependency:go-offline

# Copia il resto del codice sorgente
COPY ./greedys_api/ ./

# Build del progetto (comando Maven dinamico)
RUN mvn install

# Fase di Runtime con JDK Slim
FROM openjdk:19-jdk-slim

# Installazione di netcat
RUN apt-get update && apt-get install -y netcat

# Copia del JAR costruito dalla fase di build
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Esposizione della porta dell'applicazione
EXPOSE 8443

# Configurazione dell'entrypoint per avviare l'applicazione
ENTRYPOINT ["java", "-jar", "/app.jar"]
