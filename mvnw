#!/bin/sh
# Docker-backed Maven entry point for this repository (Java/Maven stay in Docker).
set -eu
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
module_pom=user-service/pom.xml
if [ "${1-}" = shipment-service ]; then
  shift
  module_pom=shipment-service/pom.xml
fi
if [ "${1-}" = tracking-service ]; then
  shift
  module_pom=tracking-service/pom.xml
fi
if [ "${1-}" = api-gateway ]; then
  shift
  module_pom=api-gateway/pom.xml
fi
exec docker run --rm -v "$repo_dir:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -f "$module_pom" "$@"
