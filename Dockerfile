# Stage 1: Build the application using Gradle
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle* ./
COPY src ./src
RUN gradle clean bootJar --no-daemon

# Stage 2: Run the application on a lightweight JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]