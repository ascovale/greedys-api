FROM maven:latest AS builder

WORKDIR /app/greedys_api

# Copy the source code into the container
COPY ./greedys_api/ /app/greedys_api

# Build the project
RUN mvn clean install

FROM openjdk:19-jdk-slim

# Copy the credentials file into the container
COPY ./secured/greedy-69de3-968988eeefce.json /secured/service-account.json

# Copy the built JAR file from the builder stage
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
