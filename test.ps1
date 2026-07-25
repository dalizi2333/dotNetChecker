param(
    [Parameter(Position = 0)]
    [ValidateSet("mc1211", "mc2612")]
    [string]$Version = "mc1211",

    [Parameter(Position = 1)]
    [ValidateSet("server", "client")]
    [string]$Side = "server"
)

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=== dotNetChecker Test Runner ===" -ForegroundColor Cyan
Write-Host "Target: $Version ($Side)" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build the main project JAR
Write-Host "[1/2] Building dotNetChecker..." -ForegroundColor Yellow
$jarResult = & .\gradlew.bat jar --no-daemon 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    $jarResult | Out-String | Write-Host
    exit 1
}
Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Step 2: Clean previous run artifacts (keep logs if needed)
$runDir = Join-Path $Root "runs\$Version\run"
if (Test-Path $runDir) {
    Write-Host "[2/3] Cleaning previous run artifacts..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "$runDir\mods" -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force "$runDir\world" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\server.properties" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\*.json" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\*.txt" -ErrorAction SilentlyContinue
}
Write-Host ""

# Step 3: Run the test environment
Write-Host "[3/3] Starting $Version $Side..." -ForegroundColor Yellow
Set-Location (Join-Path $Root "runs\$Version")

$taskName = if ($Side -eq "server") { "runServer" } else { "runClient" }
$sideResult = & .\gradlew.bat $taskName --no-daemon 2>&1
$sideResult | Out-String | Write-Host

# Restore original directory
Set-Location $Root
