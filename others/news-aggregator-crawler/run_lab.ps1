# Start Redis, build + run the app, run tests + demo traffic.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
docker compose up -d redis
Start-Sleep -Seconds 3
mvn -q test
Start-Job { Set-Location $using:PSScriptRoot; mvn spring-boot:run } | Out-Null
Write-Host "Waiting for app on :8080 ..."
for ($i = 0; $i -lt 30; $i++) {
  try { (Invoke-WebRequest http://localhost:8081/api/mock-urls -TimeoutSec 3).StatusCode | Out-Null; break }
  catch { Start-Sleep -Seconds 3 }
}
python scripts/gen_postman.py
python scripts/demo_requests.py all
Write-Host "Done. Swagger: http://localhost:8081/swagger-ui.html"
