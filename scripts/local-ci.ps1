# Local CI parity for developers without GitHub Actions runners
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$Jdk = Join-Path $Root ".tools\jdk-21"
if (Test-Path "$Jdk\bin\java.exe") {
  $env:JAVA_HOME = $Jdk
  $env:Path = "$Jdk\bin;" + $env:Path
}

Write-Host "== safety scan =="
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root "scripts\safety-scan.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== mvn verify =="
mvn -B -ntp clean verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== dependency tree =="
mvn -B -ntp dependency:tree "-DoutputFile=dependency-tree.txt"
Write-Host "LOCAL CI PASSED"
