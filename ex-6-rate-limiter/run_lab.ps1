param(
    [switch]$NoDocker,          # assume Redis is already running on localhost:6379
    [switch]$KeepRedis,         # leave the redis container running afterwards
    [switch]$SkipReport         # do not re-generate the report docx/charts
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=============================================="
Write-Host "Token Bucket Rate Limiter Lab Orchestration"
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
$proc = Start-Process -FilePath "java" -ArgumentList @("-jar", "target\rate-limiter-0.0.1-SNAPSHOT.jar") `
    -WorkingDirectory $Root -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

Write-Host -NoNewline "Waiting for app on http://localhost:8080 ..."
$ready = $false
$waited = 0
while ($waited -lt 90) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/hello?clientId=probe" -UseBasicParsing -TimeoutSec 3
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
    Write-Host ">>> NORMAL TRAFFIC (under the 5 tokens/s refill rate) <<<"
    python scripts/load_test.py normal --client lab-normal --requests 10 --interval 0.25

    Write-Host ""
    Write-Host ">>> BURST TRAFFIC (50 simultaneous requests, capacity 10) <<<"
    python scripts/load_test.py burst --client lab-burst --requests 50

    Write-Host ""
    Write-Host ">>> DRAIN (one-by-one until 429) <<<"
    python scripts/load_test.py drain --client lab-drain

    Write-Host ""
    Write-Host ">>> CONCURRENCY TEST (20 threads x 20 requests, atomic Lua) <<<"
    python scripts/load_test.py concurrent --client lab-concurrent --threads 20 --per-thread 20 --capacity 10 --rate 5

    Write-Host ""
    Write-Host ">>> BUCKET STATE AFTER DEMOS (refill recovery) <<<"
    python scripts/load_test.py status --client lab-normal
    python scripts/load_test.py status --client lab-burst

    # ---------- 4. Report ----------
    if (-not $SkipReport) {
        Write-Host ""
        Write-Host "Generating charts + SD_A6.docx report..."
        python scripts/gen_report.py
    }
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
Write-Host "Swagger:  http://localhost:8080/swagger-ui.html"
Write-Host "Report:   SD_A6.docx   (assets/images/*.png)"
Write-Host "Cleanup:  docker compose down"
Write-Host "=============================================="
