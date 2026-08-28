FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B

FROM eclipse-temurin:21.0.6_7-jre-alpine
RUN apk add --no-cache postgresql16-client
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
RUN addgroup -S raspel && adduser -S raspel -G raspel && \
    mkdir -p /app/backups && chown -R raspel:raspel /app
USER raspel
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
