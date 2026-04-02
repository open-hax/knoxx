# KMS Ingestion - Clojure Service
FROM clojure:temurin-21-tools-deps-alpine AS build

WORKDIR /app

# Copy deps.edn first for caching
COPY deps.edn ./
RUN clojure -P -X:uberjar

# Copy source
COPY src ./src
COPY resources ./resources

# Build uberjar
RUN clojure -M:uberjar

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy uberjar from build stage
COPY --from=build /app/target/kms-ingestion.jar /app/kms-ingestion.jar

# Create workspace mount point
RUN mkdir -p /app/workspace

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:3003/health || exit 1

# Environment
ENV PORT=3003

# Expose port
EXPOSE 3003

# Run using clojure.main from the uberjar classpath
ENTRYPOINT ["java", "-cp", "/app/kms-ingestion.jar", "clojure.main"]
CMD ["-m", "kms-ingestion.server"]
