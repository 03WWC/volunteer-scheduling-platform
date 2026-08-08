FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/ai-scheduler-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8108
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
