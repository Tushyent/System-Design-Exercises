Write-Host "========================================="
Write-Host "MongoDB Sharding Lab Orchestration Script"
Write-Host "========================================="

Write-Host "Starting Docker Compose containers..."
docker compose up -d

Write-Host "Waiting for mongod instances to be ready..."
$instances = @(
    @{ name="configsvr"; port=27019 },
    @{ name="shard1"; port=27018 },
    @{ name="shard2"; port=27028 },
    @{ name="standalone"; port=27030 }
)
foreach ($inst in $instances) {
    Write-Host -NoNewline "Waiting for $($inst.name)..."
    while ($true) {
        $result = docker exec $($inst.name) mongosh --port $($inst.port) --quiet --eval "db.adminCommand('ping').ok" 2>&1
        if ($result -match "1") { break }
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
    }
    Write-Host " Ready!"
}

Write-Host ""
Write-Host ">>> TASK 1: INITIATING REPLICA SETS (ConfigServer, Shard1, Shard2) <<<"
Write-Host "Initiating Config Server replica set..."
Get-Content scripts\init-config.js | docker exec -i configsvr mongosh --port 27019

Write-Host "Initiating Shard 1 replica set..."
Get-Content scripts\init-shard1.js | docker exec -i shard1 mongosh --port 27018

Write-Host "Initiating Shard 2 replica set..."
Get-Content scripts\init-shard2.js | docker exec -i shard2 mongosh --port 27028

Write-Host "Waiting for replica sets to elect primary nodes..."
$replicas = @(
    @{ name="configsvr"; port=27019 },
    @{ name="shard1"; port=27018 },
    @{ name="shard2"; port=27028 }
)
foreach ($rep in $replicas) {
    Write-Host -NoNewline "Waiting for $($rep.name) to be primary..."
    while ($true) {
        $result = docker exec $($rep.name) mongosh --port $($rep.port) --quiet --eval "db.hello().isWritablePrimary" 2>&1
        if ($result -match "true") { break }
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
    }
    Write-Host " Ready!"
}

Write-Host -NoNewline "Waiting for mongos router to be ready..."
while ($true) {
    $result = docker exec mongos mongosh --port 27017 --quiet --eval "db.adminCommand('ping').ok" 2>&1
    if ($result -match "1") { break }
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
}
Write-Host " Ready!"

Write-Host ""
Write-Host ">>> TASKS 2-4: ADDING SHARDS, ENABLING SHARDING & CREATING SHARDED COLLECTION <<<"
Get-Content scripts\init-router.js | docker exec -i mongos mongosh --port 27017

Write-Host ""
Write-Host ">>> EXECUTING DATA SEEDING <<<"
Get-Content scripts\seed-data.js | docker exec -i mongos mongosh --port 27017

Write-Host ""
Write-Host ">>> TASKS 5-7: EXECUTING VERIFICATION <<<"
Get-Content scripts\verify-lab.js | docker exec -i mongos mongosh --port 27017

Write-Host ""
Write-Host ">>> TASK 8: EXECUTING STANDALONE COMPARISON <<<"
Get-Content scripts\standalone-comparison.js | docker exec -i standalone mongosh --port 27030

Write-Host ""
Write-Host "========================================="
Write-Host "Lab Execution Complete!"
Write-Host "You can now copy the output above for your report."
Write-Host "If you need to explore manually, run: docker exec -it mongos mongosh --port 27017"
Write-Host "========================================="
