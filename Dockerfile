FROM node:alpine

# Install Docker, curl, and openapi-generator-cli
RUN apk add --no-cache docker openrc curl && \
    rc-update add docker boot && \
    service docker start && \
    npm install -g @openapitools/openapi-generator-cli

# Set the working directory
WORKDIR /app/greedys_api

COPY ./greedys_api/ /app/greedys_api

RUN mvn clean install

FROM openjdk:19-jdk-slim

COPY --from=builder /app/greedys_api/target/*.jar /app.jar

# Copia il file delle credenziali nel container
COPY ./secured/greedy-69de3-968988eeefce.json /secured/greedy-69de3-968988eeefce.json

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
