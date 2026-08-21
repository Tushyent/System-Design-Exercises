#!/usr/bin/env bash
# Orchestrates the autocomplete lab in Unix/macOS/Git Bash

set -e

# Parse arguments
NO_DOCKER=false
KEEP_REDIS=false
for arg in "$@"; do
  case $arg in
    --NoDocker) NO_DOCKER=true ;;
    --KeepRedis) KEEP_REDIS=true ;;
  esac
done

echo "=============================================="
echo "Autocomplete Search Lab Orchestration"
echo "=============================================="

# 1. Redis
if [ "$NO_DOCKER" = false ]; then
  echo "Starting Redis (docker compose)..."
  docker compose up -d redis
  echo -n "Waiting for Redis on localhost:6379 ..."
  waited=0
  # Check connection using bash /dev/tcp (portable across bash shells)
  while ! (echo > /dev/tcp/localhost/6379) >/dev/null 2>&1; do
    if [ $waited -gt 60 ]; then
      echo "Timed out waiting for Redis"
      exit 1
    fi
    sleep 2
    waited=$((waited + 2))
    echo -n "."
  done
  echo " Ready!"
else
  echo "Using existing Redis on localhost:6379 (NoDocker)."
fi

# 2. Build + start app
echo "Building the Spring Boot app..."
mvn -q -DskipTests package

echo "Starting the app (java -jar) ..."
rm -f app.log app.err.log
java -jar target/autocomplete-0.0.1-SNAPSHOT.jar >app.log 2>app.err.log &
APP_PID=$!

echo -n "Waiting for app on http://localhost:8080 ..."
ready=false
waited=0
while [ $waited -lt 90 ]; do
  if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/autocomplete?prefix=app" | grep -q "200"; then
    ready=true
    break
  fi
  sleep 2
  waited=$((waited + 2))
  echo -n "."
done

if [ "$ready" = false ]; then
  kill -9 $APP_PID 2>/dev/null || true
  echo "App failed to start; see app.err.log"
  exit 1
fi
echo " Ready!"

# Run demos
cleanup() {
  echo ""
  echo "Stopping the Spring Boot app..."
  kill -9 $APP_PID 2>/dev/null || true
  if [ "$KEEP_REDIS" = false ] && [ "$NO_DOCKER" = false ]; then
    echo "Stopping Redis container..."
    docker compose stop redis
  fi
}
trap cleanup EXIT

echo ""
python3 scripts/load_test.py demo

echo ""
python3 scripts/load_test.py benchmark

echo ""
python3 scripts/load_test.py concurrent --threads 10 --requests 100

echo ""
echo "Generating charts + SD_A7.docx report..."
python3 scripts/gen_report.py

echo ""
echo "=============================================="
echo "Lab Execution Complete!"
echo "Swagger Docs: http://localhost:8080/swagger-ui/index.html"
echo "Cleanup:      docker compose down"
echo "=============================================="
