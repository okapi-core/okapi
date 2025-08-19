# File: okapi-metrics/Dockerfile
FROM eclipse-temurin:21-jre

# Build-time args
ARG PORT=9000
ARG JAR_FILE=okapi-metrics-0.0.1-SNAPSHOT.jar

# Runtime env (optional): only JVM flags
ENV JAVA_OPTS=""

WORKDIR /app

# Copy the jar built by the okapi-metrics module
COPY target/${JAR_FILE} /app/okapi-metrics.jar

# Copy and enable entrypoint
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT ["/app/entrypoint.sh"]
CMD []
