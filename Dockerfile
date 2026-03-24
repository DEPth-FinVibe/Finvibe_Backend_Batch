FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p /app/logs

COPY --from=builder /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV LOG_JSON_PATH=/app/logs/application-json.log

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD kill -0 1 || exit 1

ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/logs", "-jar", "/app/app.jar"]
