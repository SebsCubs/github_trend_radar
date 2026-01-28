# Multi-stage build for Spring Boot application
# This Dockerfile assumes the build context is set to the project root
# If building from ingestor_service directory, adjust paths accordingly

FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app


COPY build.gradle.kts settings.gradle.kts ./

#(required dependency)
COPY common_models ./common_models


COPY ingestor_service ./ingestor_service

# Build the application (from root to handle multi-module project)
# Module name in settings.gradle.kts is "ingestor_service" (with underscore)
RUN gradle :ingestor_service:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from builder stage
COPY --from=builder --chown=spring:spring /app/ingestor_service/build/libs/*.jar app.jar

# Expose actuator port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with JVM optimizations for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
