param(
    [string]$AdbPath = ""
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($AdbPath)) {
    $sdkDirectory = if ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } elseif ($env:ANDROID_HOME) {
        $env:ANDROID_HOME
    } else {
        Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
    $AdbPath = Join-Path $sdkDirectory "platform-tools\adb.exe"
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb was not found at '$AdbPath'. Pass -AdbPath or configure ANDROID_SDK_ROOT."
}

Push-Location $projectDirectory
try {
    & .\gradlew.bat assembleDebug assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "The app or device-test APK did not build." }

    $deviceState = (& $AdbPath get-state 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $deviceState -ne "device") {
        throw "No ready Android device or emulator is connected."
    }

    & $AdbPath install -r "app\build\outputs\apk\debug\app-debug.apk"
    if ($LASTEXITCODE -ne 0) { throw "The app APK could not be installed." }

    & $AdbPath install -r "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ($LASTEXITCODE -ne 0) { throw "The device-test APK could not be installed." }

    & $AdbPath shell pm clear app.veshinantam | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "The app could not be reset for the fresh-install smoke check." }
    $launchOutput = (& $AdbPath shell am start -W -n "app.veshinantam/.MainActivity" 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $launchOutput -notmatch "Status:\s+ok") {
        throw "The app did not launch successfully.`n$launchOutput"
    }
    Start-Sleep -Seconds 2
    & $AdbPath shell uiautomator dump /sdcard/veshinantam-smoke.xml | Out-Null
    $hierarchy = (& $AdbPath shell cat /sdcard/veshinantam-smoke.xml 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $hierarchy -notmatch 'package="app.veshinantam"' -or $hierarchy -notmatch 'VeShinantam') {
        throw "The fresh-install onboarding screen was not exposed through the Android UI hierarchy."
    }
    & $AdbPath shell am force-stop app.veshinantam | Out-Null

    $testOutput = (& $AdbPath shell am instrument -w `
        "app.veshinantam.test/app.veshinantam.data.local.MigrationTestInstrumentation" 2>&1 | Out-String)
    $testOutput.TrimEnd() | Write-Output
    if ($LASTEXITCODE -ne 0 -or
        $testOutput -notmatch "database migrations 1-8 passed" -or
        $testOutput -notmatch "end-to-end schedule, Today, completion, and backup smoke passed") {
        throw "The on-device smoke suite failed."
    }
} finally {
    Pop-Location
}
