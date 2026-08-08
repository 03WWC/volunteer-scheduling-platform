FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/activity-service-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8101
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
