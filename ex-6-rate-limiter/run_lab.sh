#!/usr/bin/env bash
# Token Bucket Rate Limiter lab orchestration (Linux/macOS).
# Usage: ./run_lab.sh [--no-docker] [--keep-redis] [--skip-report]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

NO_DOCKER=0; KEEP_REDIS=0; SKIP_REPORT=0
for a in "$@"; do
  case "$a" in
    --no-docker) NO_DOCKER=1 ;;
    --keep-redis) KEEP_REDIS=1 ;;
    --skip-report) SKIP_REPORT=1 ;;
    *) echo "unknown arg: $a" >&2; exit 2 ;;
  esac
done

echo "=============================================="
echo "Token Bucket Rate Limiter Lab Orchestration"
echo "=============================================="

# 1. Redis
if [ "$NO_DOCKER" -eq 0 ]; then
  echo "Starting Redis (docker compose)..."
  docker compose up -d redis
  echo -n "Waiting for Redis on localhost:6379 ..."
  until (exec 3<>/dev/tcp/localhost/6379) 2>/dev/null; do sleep 2; echo -n "."; done
  echo " Ready!"
else
  echo "Using existing Redis on localhost:6379 (--no-docker)."
fi

# 2. Build + start app
echo "Building the Spring Boot app..."
mvn -q -DskipTests package
echo "Starting the app (java -jar) ..."
nohup java -jar target/rate-limiter-0.0.1-SNAPSHOT.jar > app.log 2> app.err.log &
APP_PID=$!
echo -n "Waiting for app on http://localhost:8080 ..."
for i in $(seq 1 45); do
  if curl -sf "http://localhost:8080/api/hello?clientId=probe" > /dev/null 2>&1; then break; fi
  sleep 2; echo -n "."
done
echo " Ready!"

cleanup() {
  echo ""
  echo "Stopping the Spring Boot app..."
  kill "$APP_PID" 2>/dev/null || true
  if [ "$KEEP_REDIS" -eq 0 ] && [ "$NO_DOCKER" -eq 0 ]; then
    echo "Stopping Redis container (use --keep-redis to keep it)..."
    docker compose stop redis
  fi
}
trap cleanup EXIT

# 3. Demos
echo ""
echo ">>> NORMAL TRAFFIC (under the 5 tokens/s refill rate) <<<"
python3 scripts/load_test.py normal --client lab-normal --requests 10 --interval 0.25

echo ""
echo ">>> BURST TRAFFIC (50 simultaneous requests, capacity 10) <<<"
python3 scripts/load_test.py burst --client lab-burst --requests 50

echo ""
echo ">>> DRAIN (one-by-one until 429) <<<"
python3 scripts/load_test.py drain --client lab-drain

echo ""
echo ">>> CONCURRENCY TEST (20 threads x 20 requests, atomic Lua) <<<"
python3 scripts/load_test.py concurrent --client lab-concurrent --threads 20 --per-thread 20 --capacity 10 --rate 5

echo ""
echo ">>> BUCKET STATE AFTER DEMOS (refill recovery) <<<"
python3 scripts/load_test.py status --client lab-normal
python3 scripts/load_test.py status --client lab-burst

# 4. Report
if [ "$SKIP_REPORT" -eq 0 ]; then
  echo ""
  echo "Generating charts + SD_A6.docx report..."
  python3 scripts/gen_report.py
fi

echo ""
echo "=============================================="
echo "Lab Execution Complete!"
echo "Swagger:  http://localhost:8080/swagger-ui.html"
echo "Report:   SD_A6.docx   (assets/images/*.png)"
echo "Cleanup:  docker compose down"
echo "=============================================="
