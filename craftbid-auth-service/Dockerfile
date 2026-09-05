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

ENTRYPOINT ["sh", "-c", "echo '===== JAVA VERSION ====='; java -version; echo '===== JAR CHECK ====='; ls -lh /app/app.jar; echo '===== DATASOURCE ENV CHECK ====='; if [ -n \"$SPRING_DATASOURCE_URL\" ]; then echo 'SPRING_DATASOURCE_URL = SET'; else echo 'SPRING_DATASOURCE_URL = MISSING'; fi; if [ -n \"$SPRING_DATASOURCE_USERNAME\" ]; then echo 'SPRING_DATASOURCE_USERNAME = SET'; else echo 'SPRING_DATASOURCE_USERNAME = MISSING'; fi; if [ -n \"$SPRING_DATASOURCE_PASSWORD\" ]; then echo 'SPRING_DATASOURCE_PASSWORD = SET'; else echo 'SPRING_DATASOURCE_PASSWORD = MISSING'; fi; echo '===== AIVEN TCP TEST ====='; nc -vz -w 15 craftbid-mysql1-ranjitbarik146-craftbid.a.aivencloud.com 16393; echo '===== STARTING SPRING BOOT ====='; java -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --debug 2>&1; echo '===== JAVA EXIT CODE: '$?' ====='"]