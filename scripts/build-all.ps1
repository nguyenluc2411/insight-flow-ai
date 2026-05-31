#Requires -Version 5.1
<#
.SYNOPSIS
    Build tất cả Java services theo đúng thứ tự dependency.

.DESCRIPTION
    Shared-core được build trước (install vào local .m2), sau đó tất cả services.
    Script chạy từ project root.

.PARAMETER SkipTests
    Bỏ qua tests (mặc định: true).

.PARAMETER WithTests
    Chạy tests (override SkipTests).

.PARAMETER Service
    Chỉ build một service cụ thể (vd: auth-service).
    Shared-core vẫn được build trước nếu cần.

.PARAMETER Clean
    Chạy mvn clean trước khi package.

.EXAMPLE
    .\scripts\build-all.ps1
    .\scripts\build-all.ps1 -WithTests
    .\scripts\build-all.ps1 -Service catalog-service
    .\scripts\build-all.ps1 -Service api-gateway -Clean
#>
param(
    [switch]$WithTests,
    [switch]$Clean,
    [string]$Service = ""
)

$ErrorActionPreference = "Stop"
$startTime = Get-Date

# ── Helpers ──────────────────────────────────────────────────────────────────

function Write-Header($msg) {
    Write-Host "`n$("=" * 60)" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "$("=" * 60)" -ForegroundColor Cyan
}

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Yellow }
function Write-OK($msg)   { Write-Host "   OK  $msg" -ForegroundColor Green }
function Write-FAIL($msg) { Write-Host "   FAIL $msg" -ForegroundColor Red }

function Invoke-Maven {
    param([string]$PomPath, [string]$ServiceName, [bool]$skipTests = $true)

    $goal = if ($Clean) { "clean package" } else { "package" }
    if ($skipTests) { $goal += " -DskipTests" }
    $goal += " -q"

    Write-Step "Building $ServiceName ..."
    $t = Get-Date
    & mvn -f $PomPath $goal.Split(" ")
    if ($LASTEXITCODE -ne 0) {
        Write-FAIL "$ServiceName (exit $LASTEXITCODE)"
        exit $LASTEXITCODE
    }
    $elapsed = [math]::Round(((Get-Date) - $t).TotalSeconds, 1)
    Write-OK "$ServiceName  (${elapsed}s)"
}

function Invoke-MavenInstall {
    param([string]$PomPath, [string]$LibName)

    Write-Step "Installing $LibName ..."
    $t = Get-Date
    & mvn -f $PomPath install -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-FAIL "$LibName (exit $LASTEXITCODE)"
        exit $LASTEXITCODE
    }
    $elapsed = [math]::Round(((Get-Date) - $t).TotalSeconds, 1)
    Write-OK "$LibName  (${elapsed}s)"
}

# ── Service registry ──────────────────────────────────────────────────────────

$sharedCore = @(
    @{ Path = "shared-core/common-events/pom.xml";   Name = "common-events"   },
    @{ Path = "shared-core/common-security/pom.xml"; Name = "common-security" },
    @{ Path = "shared-core/common-web/pom.xml";      Name = "common-web"      }
)

$services = [ordered]@{
    "discovery-server"    = "platform-services/discovery-server/pom.xml"
    "config-server"       = "platform-services/config-server/pom.xml"
    "api-gateway"         = "platform-services/api-gateway/pom.xml"
    "auth-service"        = "business-services/auth-service/pom.xml"
    "catalog-service"     = "business-services/catalog-service/pom.xml"
    "sales-service"       = "business-services/sales-service/pom.xml"
    "integration-service" = "integration-services/integration-service/pom.xml"
    "dashboard-bff"       = "engagement-services/dashboard-bff/pom.xml"
    "notification-service"= "engagement-services/notification-service/pom.xml"
}

# ── Main ─────────────────────────────────────────────────────────────────────

Write-Header "Insight Flow AI — Java Build"
Write-Host "  Tests:   $(if ($WithTests) { 'ON' } else { 'OFF (use -WithTests to enable)' })"
Write-Host "  Clean:   $(if ($Clean) { 'ON' } else { 'OFF' })"
Write-Host "  Service: $(if ($Service) { $Service } else { 'ALL' })"

$skipTests = -not $WithTests

# Always build shared-core (fast with .m2 cache)
Write-Header "1/2  Shared Core"
foreach ($lib in $sharedCore) {
    Invoke-MavenInstall -PomPath $lib.Path -LibName $lib.Name
}

Write-Header "2/2  Services"

if ($Service) {
    if (-not $services.Contains($Service)) {
        Write-FAIL "Unknown service '$Service'. Valid values: $($services.Keys -join ', ')"
        exit 1
    }
    Invoke-Maven -PomPath $services[$Service] -ServiceName $Service -skipTests $skipTests
} else {
    foreach ($svc in $services.GetEnumerator()) {
        Invoke-Maven -PomPath $svc.Value -ServiceName $svc.Key -skipTests $skipTests
    }
}

$total = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)
Write-Host "`n$("=" * 60)" -ForegroundColor Green
Write-Host "  BUILD SUCCESS  (total: ${total}s)" -ForegroundColor Green
Write-Host "$("=" * 60)`n" -ForegroundColor Green
