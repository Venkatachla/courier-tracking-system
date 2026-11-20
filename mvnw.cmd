@echo off
rem Docker-backed Maven entry point for this repository (Java/Maven stay in Docker).
set "MODULE_POM=user-service/pom.xml"
if /I "%~1"=="shipment-service" (
  set "MODULE_POM=shipment-service/pom.xml"
  shift
)
if /I "%~1"=="tracking-service" (
  set "MODULE_POM=tracking-service/pom.xml"
  shift
)
if /I "%~1"=="api-gateway" (
  set "MODULE_POM=api-gateway/pom.xml"
  shift
)
docker run --rm -v "%~dp0:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -f %MODULE_POM% %*
