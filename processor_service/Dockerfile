# Multi-stage build for Spring Boot application
# Build context is the project root (set in docker-compose.yml)

FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Copy root Gradle configuration files
COPY build.gradle.kts settings.gradle.kts ./

# Copy common_models module (required dependency)
COPY common_models ./common_models

# Copy processor_service module
COPY processor_service ./processor_service

# Build the application (from root to handle multi-module project)
RUN gradle :processor_service:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from builder stage
COPY --from=builder --chown=spring:spring /app/processor_service/build/libs/*.jar app.jar

# Expose actuator port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application with JVM optimizations for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
