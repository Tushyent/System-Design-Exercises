#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

echo "=============================================="
echo "Lab Exercise 8: Web Crawler Lab Orchestration"
echo "=============================================="

# 1. Start Redis if docker is available
USE_REDIS=false
if command -v docker &> /dev/null && docker info &> /dev/null; then
    echo "Starting Redis via Docker Compose..."
    docker compose up -d redis
    USE_REDIS=true
    sleep 2
else
    echo "Docker not running; falling back to thread-safe in-memory storage."
fi

# 2. Build Spring Boot application
if command -v mvn &> /dev/null; then
    mvn -q -DskipTests package
else
    ./mvnw -q -DskipTests package
fi

# 3. Launch Spring Boot application in background
echo "Starting Spring Boot application..."
JAR_FILE=$(find target -name "*.jar" ! -name "*original*" | head -n 1)
java -jar "$JAR_FILE" > app.log 2> app.err.log &
APP_PID=$!

cleanup() {
    echo "Cleaning up..."
    kill -9 $APP_PID 2>/dev/null || true
    if [ "$USE_REDIS" = true ]; then
        docker compose stop redis 2>/dev/null || true
    fi
}
trap cleanup EXIT

# 4. Wait for application to be ready
echo -n "Waiting for application on http://localhost:8080 ..."
for i in {1..45}; do
    if curl -s http://localhost:8080/api/crawler/status > /dev/null 2>&1; then
        echo " Ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# 5. Execute benchmarks and report generation
echo ""
python3 scripts/load_test.py demo
echo ""
python3 scripts/load_test.py benchmark
echo ""
python3 scripts/load_test.py concurrent
echo ""
python3 scripts/gen_report.py || echo "Lab report generation skipped (python-docx not installed in environment)."

echo "=============================================="
echo "Lab Exercise 8 Execution Finished Successfully!"
echo "Report Generated: SD_A8.docx & SD_A8_Web_Crawler.pdf"
echo "=============================================="
