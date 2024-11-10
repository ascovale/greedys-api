FROM maven:latest AS builder

WORKDIR /usr/src/app

COPY ./greedys_api/ /usr/src/app
RUN mvn clean install

FROM openjdk:19-jdk-slim

COPY --from=builder /usr/src/app/target/*.jar /app.jar

# Copia il file delle credenziali nel container
COPY ./secured/greedy-69de3-968988eeefce.json /secured/greedy-69de3-968988eeefce.json


EXPOSE 8080

ENTRYPOINT ["java"]
CMD ["-jar", "/app.jar"]
