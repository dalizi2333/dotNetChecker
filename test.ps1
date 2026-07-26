param(
    [Parameter(Position = 0)]
    [ValidateSet("mc1211", "mc2612")]
    [string]$Version = "mc1211",

    [Parameter(Position = 1)]
    [ValidateSet("server", "client")]
    [string]$Side = "server",

    [Parameter(Mandatory = $false)]
    [string]$TestFailVersion = ""
)

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "=== dotNetChecker Test Runner ===" -ForegroundColor Cyan
Write-Host "Target: $Version ($Side)" -ForegroundColor Cyan
Write-Host ""

# Use the Java already on PATH / JAVA_HOME.
# The Gradle toolchain will resolve the correct version for each test environment.
Write-Host "Java: $(& java -version 2>&1 | Select-Object -First 1)" -ForegroundColor DarkGray
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray
Write-Host ""

# Step 1: Clean previous run artifacts (keep logs if needed)
$runDir = Join-Path $Root "runs\$Version\run"
if (Test-Path $runDir) {
    Write-Host "[1/2] Cleaning previous run artifacts..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "$runDir\mods" -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force "$runDir\world" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\server.properties" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\*.json" -ErrorAction SilentlyContinue
    Remove-Item -Force "$runDir\*.txt" -ErrorAction SilentlyContinue
}
Write-Host ""

# Step 2: Build and run the test environment
# Subproject task path: :runs:<Version>:run<Side>
$subprojectPath = ":runs:$Version`:run$($Side.substring(0,1).toupper()+$Side.substring(1))"
Write-Host "[2/2] Starting $Version $Side..." -ForegroundColor Yellow

# Build Gradle args
$gradleArgs = @($subprojectPath, "--no-daemon")
if ($TestFailVersion) {
    $jvmArgs = "-Ddotnetchecker.test.requireVersion=$TestFailVersion -Ddotnetchecker.test.requireModId=testrunner"
    $gradleArgs += "-PdotNetChecker.jvmArgs=$jvmArgs"
    Write-Host "Test mode: injecting requirement >= $TestFailVersion" -ForegroundColor Magenta
}
Write-Host "Running: gradlew $($gradleArgs -join ' ')" -ForegroundColor DarkGray

$sideResult = & .\gradlew.bat $gradleArgs 2>&1
$sideResult | Out-String | Write-Host
