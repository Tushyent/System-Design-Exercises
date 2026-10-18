# One-Click Startup Script for Vidya LMS (Scalable Library Management System)
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Starting Vidya LMS - Scalable Library System           " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verify MongoDB Port 27017
Write-Host "`n[1/3] Checking MongoDB on localhost:27017..." -ForegroundColor Yellow
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect("127.0.0.1", 27017)
    $tcp.Close()
    Write-Host " -> MongoDB is active and accepting connections." -ForegroundColor Green
} catch {
    Write-Host " -> WARNING: MongoDB port 27017 not connectable. Starting MongoDB Windows service..." -ForegroundColor Yellow
    Start-Service -Name MongoDB -ErrorAction SilentlyContinue
}

# 2. Check Backend JAR
$jarPath = Join-Path $PSScriptRoot "backend\target\scalable-library-system-1.0.0.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "`n[2/3] Compiling backend and packaging JAR..." -ForegroundColor Yellow
    Push-Location (Join-Path $PSScriptRoot "backend")
    mvn package -DskipTests
    Pop-Location
} else {
    Write-Host "`n[2/3] Backend JAR already exists: $jarPath" -ForegroundColor Green
}

# 3. Launch Backend in new window
Write-Host "`nLaunching Spring Boot backend on http://localhost:8080..." -ForegroundColor Cyan
Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\backend'; java -jar target\scalable-library-system-1.0.0.jar"

# 4. Launch Frontend Vite Dev Server
Write-Host "`nLaunching React frontend on http://localhost:5173..." -ForegroundColor Cyan
Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\frontend'; npm run dev"

Write-Host "`nSystem successfully launched!" -ForegroundColor Green
Write-Host "Backend API / Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "Frontend Application:  http://localhost:5173" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
