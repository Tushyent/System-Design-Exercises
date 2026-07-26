#!/bin/bash
echo "========================================="
echo "MongoDB Sharding Lab Orchestration Script"
echo "========================================="

echo "Starting Docker Compose containers..."
docker compose up -d

echo "Waiting for mongod instances to be ready..."
for container in configsvr shard1 shard2 standalone; do
    port=27019
    if [ "$container" == "shard1" ]; then port=27018; fi
    if [ "$container" == "shard2" ]; then port=27028; fi
    if [ "$container" == "standalone" ]; then port=27030; fi
    echo -n "Waiting for $container..."
    until docker exec $container mongosh --port $port --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null | grep -q "1"; do
        echo -n "."
        sleep 2
    done
    echo " Ready!"
done

echo ""
echo ">>> TASK 1: INITIATING REPLICA SETS (ConfigServer, Shard1, Shard2) <<<"
echo "Initiating Config Server replica set..."
docker exec -i configsvr mongosh --port 27019 < scripts/init-config.js

echo "Initiating Shard 1 replica set..."
docker exec -i shard1 mongosh --port 27018 < scripts/init-shard1.js

echo "Initiating Shard 2 replica set..."
docker exec -i shard2 mongosh --port 27028 < scripts/init-shard2.js

echo "Waiting for replica sets to elect primary nodes..."
for container in configsvr shard1 shard2; do
    port=27019
    if [ "$container" == "shard1" ]; then port=27018; fi
    if [ "$container" == "shard2" ]; then port=27028; fi
    echo -n "Waiting for $container to be primary..."
    until docker exec $container mongosh --port $port --quiet --eval "db.hello().isWritablePrimary" 2>/dev/null | grep -q "true"; do
        echo -n "."
        sleep 2
    done
    echo " Ready!"
done

echo -n "Waiting for mongos router to be ready..."
until docker exec mongos mongosh --port 27017 --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null | grep -q "1"; do
    echo -n "."
    sleep 2
done
echo " Ready!"

echo ""
echo ">>> TASKS 2-4: ADDING SHARDS, ENABLING SHARDING & CREATING SHARDED COLLECTION <<<"
docker exec -i mongos mongosh --port 27017 < scripts/init-router.js

echo ""
echo ">>> EXECUTING DATA SEEDING <<<"
docker exec -i mongos mongosh --port 27017 < scripts/seed-data.js

echo ""
echo ">>> TASKS 5-7: EXECUTING VERIFICATION <<<"
docker exec -i mongos mongosh --port 27017 < scripts/verify-lab.js

echo ""
echo ">>> TASK 8: EXECUTING STANDALONE COMPARISON <<<"
docker exec -i standalone mongosh --port 27030 < scripts/standalone-comparison.js

echo ""
echo "========================================="
echo "Lab Execution Complete!"
echo "You can now copy the output above for your report."
echo "If you need to explore manually, run: docker exec -it mongos mongosh --port 27017"
echo "========================================="
