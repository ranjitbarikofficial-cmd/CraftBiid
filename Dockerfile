# Multi-stage Dockerfile for CraftBid Spring Boot Backend on Render

# 1. Build JAR with Maven & OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY craftbid-auth-service/pom.xml ./
COPY craftbid-auth-service/src ./src
RUN mvn clean package -DskipTests

# 2. Lightweight Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar /app/app.jar
RUN mkdir -p /app/uploads
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8081} -Dserver.address=0.0.0.0 -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
