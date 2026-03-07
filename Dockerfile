FROM openjdk:27-ea-3-slim
WORKDIR /app
COPY build/libs/storage_app-0.0.1-SNAPSHOT.jar storage_app.jar
ENTRYPOINT ["java", "-jar", "storage_app.jar"]