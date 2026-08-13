# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN groupadd -r app && useradd -r -g app app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
