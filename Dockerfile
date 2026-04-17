FROM amazoncorretto:17-alpine
WORKDIR /app
COPY target/library-management-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
