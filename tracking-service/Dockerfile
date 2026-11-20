# Build from repository root: docker build -f tracking-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY tracking-service/pom.xml ./pom.xml
COPY tracking-service/src ./tracking-service/src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/tracking-service-1.0.0.jar app.jar
EXPOSE 5003
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
