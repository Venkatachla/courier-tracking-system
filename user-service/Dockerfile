# Build from repository root: docker build -f user-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY user-service/pom.xml ./pom.xml
COPY user-service/src ./user-service/src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/user-service-1.0.0.jar app.jar
EXPOSE 5001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
