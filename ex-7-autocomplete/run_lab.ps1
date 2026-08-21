param(
    [switch]$NoDocker,          # assume Redis is already running on localhost:6379
    [switch]$KeepRedis          # leave the redis container running afterwards
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=============================================="
Write-Host "Autocomplete Search Lab Orchestration"
Write-Host "=============================================="

# ---------- 1. Redis ----------
if (-not $NoDocker) {
    Write-Host "Starting Redis (docker compose)..."
    docker compose up -d redis
    Write-Host -NoNewline "Waiting for Redis on localhost:6379 ..."
    $waited = 0
    while ($true) {
        if ($waited -gt 60) { throw "Timed out waiting for Redis" }
        try {
            $c = New-Object System.Net.Sockets.TcpClient
            if ($c.ConnectAsync("localhost", 6379).Wait(1500) -and $c.Connected) { $c.Close(); break }
            $c.Close()
        } catch { }
        Start-Sleep -Seconds 2; $waited += 2; Write-Host -NoNewline "."
    }
    Write-Host " Ready!"
} else {
    Write-Host "Using existing Redis on localhost:6379 (NoDocker)."
}

# ---------- 2. Build + start app ----------
Write-Host "Building the Spring Boot app..."
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "Starting the app (java -jar) ..."
$out = Join-Path $Root "app.log"
$err = Join-Path $Root "app.err.log"
Remove-Item -ErrorAction SilentlyContinue $out, $err
$proc = Start-Process -FilePath "java" -ArgumentList @("-jar", "target\autocomplete-0.0.1-SNAPSHOT.jar") `
    -WorkingDirectory $Root -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

Write-Host -NoNewline "Waiting for app on http://localhost:8080 ..."
$ready = $false
$waited = 0
while ($waited -lt 90) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/autocomplete?prefix=app" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 2; $waited += 2; Write-Host -NoNewline "."
}
if (-not $ready) {
    Stop-Process -Id $proc.Id -ErrorAction SilentlyContinue
    throw "App failed to start; see app.err.log"
}
Write-Host " Ready!"

try {
    # ---------- 3. Demos ----------
    Write-Host ""
    python scripts/load_test.py demo

    Write-Host ""
    python scripts/load_test.py benchmark

    Write-Host ""
    python scripts/load_test.py concurrent --threads 10 --requests 100

    Write-Host ""
    Write-Host "Generating charts + SD_A7.docx report..."
    python scripts/gen_report.py
}
finally {
    Write-Host ""
    Write-Host "Stopping the Spring Boot app..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    if (-not $KeepRedis -and -not $NoDocker) {
        Write-Host "Stopping Redis container (use -KeepRedis to keep it)..."
        docker compose stop redis
    }
}

Write-Host ""
Write-Host "=============================================="
Write-Host "Lab Execution Complete!"
Write-Host "Swagger Docs: http://localhost:8080/swagger-ui/index.html"
Write-Host "Cleanup:      docker compose down"
Write-Host "=============================================="
