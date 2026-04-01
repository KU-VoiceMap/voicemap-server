# Stage 1: Build
FROM gradle:jdk25 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :voicemap-api:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

COPY --from=build /app/voicemap-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-Xms4g", \
    "-Xmx4g", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/heapdump.hprof", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod", \
    "--spring.config.additional-location=file:/app/config/"]
