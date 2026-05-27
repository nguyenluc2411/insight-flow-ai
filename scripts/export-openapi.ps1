#Requires -Version 5.1
<#
.SYNOPSIS
    Export OpenAPI specs từ các services đang chạy vào api-contracts/.

.DESCRIPTION
    Gọi /v3/api-docs.yaml trên từng service và lưu vào api-contracts/{service}.yaml.
    Services phải đang chạy (local hoặc Docker).

    Chạy từ project root.

.PARAMETER BaseUrl
    Base URL của API Gateway (mặc định http://localhost:8080).
    Gateway aggregate tất cả specs tại /v3/api-docs/{service-name}.
    Nếu -Direct thì gọi thẳng từng service port.

.PARAMETER Direct
    Gọi thẳng từng service thay vì qua Gateway.

.PARAMETER Service
    Chỉ export một service cụ thể.

.PARAMETER WaitSeconds
    Số giây chờ nếu service chưa sẵn sàng (mặc định: 0).

.EXAMPLE
    .\scripts\export-openapi.ps1
    .\scripts\export-openapi.ps1 -Direct
    .\scripts\export-openapi.ps1 -Service auth-service
    .\scripts\export-openapi.ps1 -Direct -WaitSeconds 30
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$Direct,
    [string]$Service = "",
    [int]$WaitSeconds = 0
)

$ErrorActionPreference = "Stop"
$outputDir = "api-contracts"

# ── Service registry ──────────────────────────────────────────────────────────
# name: logical service name (used in filename + gateway route)
# port: service port for -Direct mode

$services = @(
    @{ Name = "auth-service";         Port = 8081 },
    @{ Name = "catalog-service";      Port = 8082 },
    @{ Name = "sales-service";        Port = 8083 },
    @{ Name = "integration-service";  Port = 8084 },
    @{ Name = "dashboard-bff";        Port = 8090 },
    @{ Name = "notification-service"; Port = 8091 }
    # ml-service uses FastAPI — spec at /openapi.json (not SpringDoc)
)

$mlService = @{ Name = "ml-service"; Port = 8000 }

# ── Helpers ───────────────────────────────────────────────────────────────────

function Write-Header($msg) {
    Write-Host "`n$("=" * 60)" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "$("=" * 60)" -ForegroundColor Cyan
}

function Write-OK($msg)   { Write-Host "  [OK]   $msg" -ForegroundColor Green }
function Write-FAIL($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red }
function Write-Skip($msg) { Write-Host "  [SKIP] $msg" -ForegroundColor Gray }

function Wait-ForService([string]$url, [int]$seconds) {
    if ($seconds -le 0) { return }
    Write-Host "  Waiting up to ${seconds}s for $url ..." -ForegroundColor Yellow
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $null = Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            return
        } catch { Start-Sleep -Seconds 2 }
    }
    Write-Host "  Timeout waiting for $url" -ForegroundColor Yellow
}

function Export-Spec([string]$name, [string]$url, [string]$outFile) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
        $content  = $response.Content

        # SpringDoc returns JSON for /v3/api-docs, YAML for /v3/api-docs.yaml
        # FastAPI returns JSON for /openapi.json — convert header only
        $outPath = Join-Path $outputDir $outFile
        [System.IO.File]::WriteAllText(
            (Join-Path (Get-Location) $outPath),
            $content,
            [System.Text.Encoding]::UTF8
        )
        $sizeKb = [math]::Round($content.Length / 1024, 1)
        Write-OK "$name  →  $outPath  (${sizeKb} KB)"
    } catch {
        Write-FAIL "$name  —  $($_.Exception.Message)"
    }
}

# ── Main ──────────────────────────────────────────────────────────────────────

Write-Header "Insight Flow AI — Export OpenAPI Specs"
Write-Host "  Mode:   $(if ($Direct) { 'Direct (per-service ports)' } else { 'Gateway aggregate' })"
Write-Host "  Output: $outputDir/"

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

# Filter to single service if requested
$targets = if ($Service) {
    $services | Where-Object { $_.Name -eq $Service }
} else {
    $services
}

if ($Service -and -not $targets) {
    Write-FAIL "Unknown service '$Service'. Valid: $($services.Name -join ', '), ml-service"
    exit 1
}

# Wait for gateway / first service to be ready
if ($WaitSeconds -gt 0) {
    $checkUrl = if ($Direct) { "http://localhost:$($targets[0].Port)/actuator/health" } else { "$BaseUrl/actuator/health" }
    Wait-ForService -url $checkUrl -seconds $WaitSeconds
}

Write-Host ""

foreach ($svc in $targets) {
    $url = if ($Direct) {
        "http://localhost:$($svc.Port)/v3/api-docs.yaml"
    } else {
        "$BaseUrl/v3/api-docs/$($svc.Name)"
    }
    Export-Spec -name $svc.Name -url $url -outFile "$($svc.Name).yaml"
}

# ml-service: FastAPI serves /openapi.json (JSON, not YAML)
if (-not $Service -or $Service -eq "ml-service") {
    $mlUrl = "http://localhost:$($mlService.Port)/openapi.json"
    Export-Spec -name "ml-service" -url $mlUrl -outFile "ml-service.json"
}

Write-Host ""
Write-Header "Done"
Write-Host "  Specs saved to $outputDir/"
Write-Host "  Commit api-contracts/ and notify frontend team if public API changed."
Write-Host ""
