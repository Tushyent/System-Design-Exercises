param(
    [switch]$NoDocker,          # assume Redis is already running on localhost:6379 or fallback to in-memory
    [switch]$KeepRedis          # leave the redis container running afterwards
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=============================================="
Write-Host "Lab Exercise 8: Web Crawler Lab Orchestration"
Write-Host "=============================================="

# ---------- 1. Redis (Docker or In-Memory Fallback) ----------
$useRedis = $false
if (-not $NoDocker) {
    try {
        docker info > $null 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Starting Redis (docker compose)..."
            docker compose up -d redis
            Write-Host -NoNewline "Waiting for Redis on localhost:6379 ..."
            $waited = 0
            while ($waited -lt 30) {
                try {
                    $c = New-Object System.Net.Sockets.TcpClient
                    if ($c.ConnectAsync("localhost", 6379).Wait(1500) -and $c.Connected) { $c.Close(); $useRedis = $true; break }
                    $c.Close()
                } catch { }
                Start-Sleep -Seconds 2; $waited += 2; Write-Host -NoNewline "."
            }
            if ($useRedis) { Write-Host " Ready!" } else { Write-Host " (Using in-memory fallback)" }
        } else {
            Write-Host "Docker daemon not running; Web Crawler will run using thread-safe In-Memory fallback."
        }
    } catch {
        Write-Host "Docker not accessible; Web Crawler will run using thread-safe In-Memory fallback."
    }
} else {
    Write-Host "NoDocker specified. Connecting to existing Redis or in-memory fallback."
}

# ---------- 2. Build + Start Spring Boot App ----------
Write-Host "Building the Spring Boot app..."
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "Starting the app (java -jar) ..."
$out = Join-Path $Root "app.log"
$err = Join-Path $Root "app.err.log"
Remove-Item -ErrorAction SilentlyContinue $out, $err

$jar = Get-ChildItem -Path (Join-Path $Root "target") -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
if (-not $jar) { throw "Build artifact not found in target/" }

$proc = Start-Process -FilePath "java" -ArgumentList @("-jar", $jar.FullName) `
    -WorkingDirectory $Root -RedirectStandardOutput $out -RedirectStandardError $err -PassThru -WindowStyle Hidden

Write-Host -NoNewline "Waiting for app on http://localhost:8080 ..."
$ready = $false
$waited = 0
while ($waited -lt 90) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/crawler/status" -UseBasicParsing -TimeoutSec 3
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
    # ---------- 3. Demos & Benchmarks ----------
    Write-Host ""
    python scripts/load_test.py demo

    Write-Host ""
    python scripts/load_test.py benchmark

    Write-Host ""
    python scripts/load_test.py concurrent

    Write-Host ""
    Write-Host "Generating charts + SD_A8.docx report..."
    python scripts/gen_report.py
}
finally {
    Write-Host ""
    Write-Host "Stopping the Spring Boot app..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    if ($useRedis -and -not $KeepRedis) {
        Write-Host "Stopping Redis container..."
        docker compose stop redis
    }
}

Write-Host ""
Write-Host "=============================================="
Write-Host "Lab Exercise 8 Execution Complete!"
Write-Host "Report Generated : SD_A8.docx & SD_A8_Web_Crawler.pdf"
Write-Host "Web UI Dashboard : http://localhost:8080/"
Write-Host "Swagger Docs     : http://localhost:8080/swagger-ui/index.html"
Write-Host "=============================================="
