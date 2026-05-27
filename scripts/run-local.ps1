#Requires -Version 5.1
<#
.SYNOPSIS
    Khởi động môi trường local cho Insight Flow AI.

.DESCRIPTION
    Hai chế độ:
      Infra  (mặc định) — chỉ start infra (Postgres, Kafka, Redis, MinIO, Observability...).
                          Services chạy từ IDE / mvnw spring-boot:run.
      Full              — start cả infra lẫn 10 services trong Docker.
                          Cần build JAR trước: .\scripts\build-all.ps1

.PARAMETER Full
    Start toàn bộ stack (infra + services).

.PARAMETER Down
    Stop và remove tất cả containers.

.PARAMETER Logs
    Xem logs của một service cụ thể sau khi start.

.EXAMPLE
    .\scripts\run-local.ps1               # Infra only
    .\scripts\run-local.ps1 -Full         # Full stack
    .\scripts\run-local.ps1 -Down         # Stop all
    .\scripts\run-local.ps1 -Logs kafka   # Start infra + xem kafka logs
#>
param(
    [switch]$Full,
    [switch]$Down,
    [string]$Logs = ""
)

$ErrorActionPreference = "Stop"

$COMPOSE_INFRA    = "infrastructure/docker/docker-compose.yml"
$COMPOSE_SERVICES = "infrastructure/docker/docker-compose.services.yml"
$ENV_FILE         = ".env"
$ENV_EXAMPLE      = ".env.example"

# ── Helpers ──────────────────────────────────────────────────────────────────

function Write-Header($msg) {
    Write-Host "`n$("=" * 60)" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "$("=" * 60)" -ForegroundColor Cyan
}

function Write-Info($msg)  { Write-Host "  $msg" -ForegroundColor White }
function Write-OK($msg)    { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "  [!!] $msg" -ForegroundColor Yellow }
function Write-Error2($msg){ Write-Host "  [ERR] $msg" -ForegroundColor Red }

# ── Pre-flight checks ─────────────────────────────────────────────────────────

if (-not (Get-Command "docker" -ErrorAction SilentlyContinue)) {
    Write-Error2 "Docker not found. Install Docker Desktop and try again."
    exit 1
}

$dockerRunning = docker info 2>&1 | Select-String "Server Version"
if (-not $dockerRunning) {
    Write-Error2 "Docker daemon is not running. Start Docker Desktop first."
    exit 1
}

if (-not (Test-Path $ENV_FILE)) {
    Write-Warn ".env not found — copying from .env.example"
    Copy-Item $ENV_EXAMPLE $ENV_FILE
    Write-Warn "Edit .env and set real credentials before continuing."
    Write-Warn "At minimum: POSTGRES_USER, POSTGRES_PASSWORD, JWT_SECRET"
    $confirm = Read-Host "  Continue with example values? [y/N]"
    if ($confirm -notmatch "^[yY]$") { exit 0 }
}

# ── Down ─────────────────────────────────────────────────────────────────────

if ($Down) {
    Write-Header "Stopping all containers"
    if ($Full) {
        docker-compose -f $COMPOSE_INFRA -f $COMPOSE_SERVICES --env-file $ENV_FILE down --remove-orphans
    } else {
        docker-compose -f $COMPOSE_INFRA --env-file $ENV_FILE down --remove-orphans
    }
    Write-OK "All containers stopped."
    exit 0
}

# ── Start ─────────────────────────────────────────────────────────────────────

if ($Full) {
    Write-Header "Starting full stack (infra + services)"
    Write-Warn "Ensure JARs are built: .\scripts\build-all.ps1"
    docker-compose -f $COMPOSE_INFRA -f $COMPOSE_SERVICES --env-file $ENV_FILE up -d --remove-orphans
} else {
    Write-Header "Starting infra only"
    docker-compose -f $COMPOSE_INFRA --env-file $ENV_FILE up -d --remove-orphans
}

if ($LASTEXITCODE -ne 0) {
    Write-Error2 "docker-compose failed (exit $LASTEXITCODE)"
    exit $LASTEXITCODE
}

# ── Status ────────────────────────────────────────────────────────────────────

Write-Header "Container status"
docker-compose -f $COMPOSE_INFRA --env-file $ENV_FILE ps

Write-Header "Access points"
Write-Info "Postgres        localhost:5433"
Write-Info "Kafka           localhost:9092  (internal: kafka:29092)"
Write-Info "Kafka UI        http://localhost:8085"
Write-Info "Redis           localhost:6379"
Write-Info "MinIO API       http://localhost:9000"
Write-Info "MinIO Console   http://localhost:9001  (minioadmin / see .env)"
Write-Info "MailHog         http://localhost:8025"
Write-Info "pgAdmin         http://localhost:5050"
Write-Info "Prometheus      http://localhost:9090"
Write-Info "Grafana         http://localhost:3001  (admin / see .env)"

if ($Full) {
    Write-Host ""
    Write-Info "API Gateway     http://localhost:8080"
    Write-Info "Eureka          http://localhost:8761"
    Write-Info "Swagger (gateway aggregate)  http://localhost:8080/swagger-ui.html"
}

Write-Host ""

# ── Logs ─────────────────────────────────────────────────────────────────────

if ($Logs) {
    Write-Header "Logs: $Logs  (Ctrl+C to exit)"
    docker-compose -f $COMPOSE_INFRA --env-file $ENV_FILE logs -f $Logs
}
