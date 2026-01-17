FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN  mvn -pl authentification clean package -DskipTests -am

# Étape 2 : Construire l'image minimale
FROM eclipse-temurin:17-alpine
WORKDIR /app
COPY --from=builder /app/authentification/target/*.jar authentification.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "authentification.jar"]
