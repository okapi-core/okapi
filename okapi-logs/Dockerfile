FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the prepackaged Spring Boot fat jar
ARG JAR_FILE=target/okapi-logs-*.jar
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]