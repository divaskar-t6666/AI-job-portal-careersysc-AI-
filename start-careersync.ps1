$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'pom.xml')) -or
    -not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'mvnw.cmd'))) {
    Write-Host 'ERROR: Open/run this script from the extracted folder containing pom.xml and mvnw.cmd.' -ForegroundColor Red
    exit 1
}

$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($null -ne $listener) {
    $listenerPid = [int]$listener.OwningProcess
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $listenerPid" -ErrorAction SilentlyContinue
    Write-Host ''
    Write-Host 'Port 8080 is already occupied. This can be the old/different backend that caused the Career AI 403.' -ForegroundColor Yellow
    Write-Host ("PID: {0}; Process: {1}; Executable: {2}" -f $listenerPid, $processInfo.Name, $processInfo.ExecutablePath)
    Write-Host 'Only continue if this is the old CareerSync/Spring Boot backend you want to replace.' -ForegroundColor Yellow
    $answer = Read-Host 'Type YES to stop that process and launch this project; anything else cancels'
    if ($answer -cne 'YES') {
        Write-Host 'Cancelled. No process was stopped.'
        exit 1
    }
    Stop-Process -Id $listenerPid -Force
    Start-Sleep -Seconds 2
}

Write-Host ''
Write-Host 'Starting CareerSync AI from this exact workspace:' -ForegroundColor Cyan
Write-Host $PSScriptRoot
Write-Host 'Wait until Spring Boot reports that Tomcat started on port 8080.' -ForegroundColor Cyan
Write-Host 'First run may need internet access to download Maven dependencies.' -ForegroundColor Yellow
& (Join-Path $PSScriptRoot 'mvnw.cmd') clean spring-boot:run
exit $LASTEXITCODE
