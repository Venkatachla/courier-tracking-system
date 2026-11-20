# Build from repository root: docker build -f shipment-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY shipment-service/pom.xml ./pom.xml
COPY shipment-service/src ./shipment-service/src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/shipment-service-1.0.0.jar app.jar
EXPOSE 5002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
