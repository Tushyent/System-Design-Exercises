#!/bin/bash
echo "=============================================="
echo "Consistent Hashing Lab Orchestration Script"
echo "=============================================="

echo "Starting MongoDB storage nodes (mongo1..mongo4)..."
docker compose up -d mongo1 mongo2 mongo3 mongo4

echo "Waiting for mongod instances to be ready..."
for node in mongo1 mongo2 mongo3 mongo4; do
    echo -n "Waiting for $node..."
    until docker exec $node mongosh --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null | grep -q "1"; do
        echo -n "."
        sleep 2
    done
    echo " Ready!"
done

echo ""
echo "Building & starting the Spring Boot app (docker)..."
docker compose up -d --build app

echo -n "Waiting for app on http://localhost:8080 ..."
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/distribution 2>/dev/null | grep -q "200"; do
    echo -n "."
    sleep 2
done
echo " Ready!"

echo ""
echo ">>> DEMO: INITIAL DISTRIBUTION (seeded 20 students across 3 nodes) <<<"
curl -s http://localhost:8080/api/distribution | jq '{total, counts}'

echo ""
echo ">>> DEMO: ADD STORAGE NODE mongo4 (only ~1/N records migrate) <<<"
curl -s -X POST http://localhost:8080/api/nodes \
  -H 'Content-Type: application/json' \
  -d '{"host":"mongo4","port":27020}' | jq '{migrated, before, after}'

echo "Distribution after add:"
curl -s http://localhost:8080/api/distribution | jq '{total, counts}'

echo ""
echo ">>> DEMO: REMOVE STORAGE NODE mongo4 (only its records redistribute) <<<"
curl -s -X DELETE http://localhost:8080/api/nodes/mongo4/27020 | jq '{migrated, before, after}'

echo "Distribution after remove (should match initial 7/6/7):"
curl -s http://localhost:8080/api/distribution | jq '{total, counts}'

echo ""
echo "=============================================="
echo "Lab Execution Complete!"
echo "UI:    http://localhost:8080/"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "mongo-express GUIs: http://localhost:8081..8084 (admin/admin)"
echo "Cleanup: docker compose down -v"
echo "=============================================="
