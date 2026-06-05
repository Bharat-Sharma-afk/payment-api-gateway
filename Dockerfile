# --- Stage 1: Build the application ---
    FROM eclipse-temurin:17-jdk-jammy AS build
    WORKDIR /app
    
    # Copy the wrapper and project files
    COPY gradlew .
    COPY gradle gradle
    COPY build.gradle .
    COPY settings.gradle .
    COPY src src
    
    # Make the wrapper executable and build the project (skipping tests)
    RUN chmod +x ./gradlew
    RUN ./gradlew clean build -x test
    
    # --- Stage 2: Run the application ---
    FROM eclipse-temurin:17-jre-jammy
    WORKDIR /app
    
    # Copy the compiled JAR file from the build stage
    # Note: Render exposes port 10000 by default, so we expose 8080 here and Render maps it.
    COPY --from=build /app/build/libs/*.jar app.jar
    
    EXPOSE 8080
    
    # Start the application
    ENTRYPOINT ["java", "-jar", "app.jar"]