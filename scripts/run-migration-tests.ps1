param(
    [string]$AdbPath = ""
)

$smokeScript = Join-Path $PSScriptRoot "run-device-smoke-tests.ps1"
& $smokeScript -AdbPath $AdbPath
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
