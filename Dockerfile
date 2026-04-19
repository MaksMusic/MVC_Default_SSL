# Сборка приложения для docker-compose.yml (локальный app + db)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace
RUN apk add --no-cache bash
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test \
    && JAR=$(ls build/libs/*.jar | grep -v '\-plain\.jar$' | head -n1) \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S javauser && adduser -S javauser -G javauser
COPY --from=builder /workspace/app.jar /app/app.jar
USER javauser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
