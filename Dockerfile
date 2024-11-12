FROM maven:latest AS builder

WORKDIR /app/greedys_api

# Copy the source code into the container
COPY ./greedys_api/ /app/greedys_api

# Build the project
RUN mvn clean install

FROM openjdk:19-jdk-slim

# Accept build arguments
ARG SPRING_PROFILES_ACTIVE
ARG KEYSTORE_PASSWORD
ARG DB_PASSWORD
ARG MAIL_PASSWORD
ARG RABBITMQ_PASSWORD

# Set environment variables from build arguments
ENV SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}"
ENV KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD}"
ENV SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/greedys_v1"
ENV SPRING_DATASOURCE_USERNAME="root"
ENV DB_PASSWORD="${DB_PASSWORD}"
ENV MAIL_PASSWORD="${MAIL_PASSWORD}"
ENV RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD}"

# Copy the credentials file into the container
COPY ./secured/greedy-69de3-968988eeefce.json /secured/service-account.json

# Copy the keystore into the container
COPY ./keystore.p12 /app/keystore.p12

# Copy the built JAR file from the builder stage
COPY --from=builder /app/greedys_api/target/*.jar /app.jar

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "/app.jar"]
