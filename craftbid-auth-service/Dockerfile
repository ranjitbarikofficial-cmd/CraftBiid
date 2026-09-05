FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

RUN mkdir -p /app/uploads

EXPOSE 8081

ENV PORT=8081

ENTRYPOINT ["sh", "-c", "exec java -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]