FROM eclipse-temurin:17-jre
WORKDIR /app
COPY health-check-worker/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
