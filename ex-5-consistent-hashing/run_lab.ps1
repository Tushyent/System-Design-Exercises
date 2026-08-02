Write-Host "=============================================="
Write-Host "Consistent Hashing Lab Orchestration Script"
Write-Host "=============================================="

Write-Host "Starting MongoDB storage nodes (mongo1..mongo4)..."
docker compose up -d mongo1 mongo2 mongo3 mongo4

Write-Host "Waiting for mongod instances to be ready..."
$nodes = @(
    @{ name="mongo1"; port=27017 },
    @{ name="mongo2"; port=27018 },
    @{ name="mongo3"; port=27019 },
    @{ name="mongo4"; port=27020 }
)
foreach ($node in $nodes) {
    Write-Host -NoNewline "Waiting for $($node.name)..."
    while ($true) {
        $result = docker exec $($node.name) mongosh --quiet --eval "db.adminCommand('ping').ok" 2>&1
        if ($result -match "1") { break }
        Write-Host -NoNewline "."
        Start-Sleep -Seconds 2
    }
    Write-Host " Ready!"
}

Write-Host ""
Write-Host "Building & starting the Spring Boot app (docker)..."
docker compose up -d --build app

Write-Host -NoNewline "Waiting for app on http://localhost:8080 ..."
while ($true) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/distribution" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -eq 200) { break }
    } catch { }
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
}
Write-Host " Ready!"

Write-Host ""
Write-Host ">>> DEMO: INITIAL DISTRIBUTION (seeded 20 students across 3 nodes) <<<"
Invoke-RestMethod -Uri "http://localhost:8080/api/distribution" | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host ">>> DEMO: ADD STORAGE NODE mongo4 (only ~1/N records migrate) <<<"
Invoke-RestMethod -Uri "http://localhost:8080/api/nodes" `
    -Method Post `
    -ContentType "application/json" `
    -Body '{"host":"mongo4","port":27020}' | ConvertTo-Json -Depth 5

Write-Host "Distribution after add:"
Invoke-RestMethod -Uri "http://localhost:8080/api/distribution" | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host ">>> DEMO: REMOVE STORAGE NODE mongo4 (only its records redistribute) <<<"
Invoke-RestMethod -Uri "http://localhost:8080/api/nodes/mongo4/27020" `
    -Method Delete | ConvertTo-Json -Depth 5

Write-Host "Distribution after remove (should match initial 7/6/7):"
Invoke-RestMethod -Uri "http://localhost:8080/api/distribution" | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "=============================================="
Write-Host "Lab Execution Complete!"
Write-Host "UI:    http://localhost:8080/"
Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html"
Write-Host "mongo-express GUIs: http://localhost:8081..8084 (admin/admin)"
Write-Host "Cleanup: docker compose down -v"
Write-Host "=============================================="
