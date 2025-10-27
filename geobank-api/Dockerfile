# GeoBank - Spatial Banking Intelligence Platform
# Multi-stage Docker build for production deployment

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime

# Add application user for security
RUN addgroup -g 1001 -S geobank && \
    adduser -u 1001 -S geobank -G geobank

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/geobank-api-*.jar app.jar

# Change ownership to non-root user
RUN chown -R geobank:geobank /app
USER geobank

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
