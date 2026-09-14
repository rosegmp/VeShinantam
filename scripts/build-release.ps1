param(
    [switch]$AllowUnsigned,
    [switch]$EntitySyncEnabled,
    [switch]$SkipLint,
    [switch]$VerifyReproducible,
    [string]$OutputDirectory = "artifacts"
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $PSScriptRoot
$outputPath = if ([System.IO.Path]::IsPathRooted($OutputDirectory)) {
    $OutputDirectory
} else {
    Join-Path $projectDirectory $OutputDirectory
}

function Invoke-ReleaseBuild {
    $arguments = @("clean", "testDebugUnitTest", "assembleRelease")
    if ($EntitySyncEnabled) {
        $arguments += "-PentitySyncEnabled=true"
    }
    if ($SkipLint) {
        $arguments += @(
            "-x", ":app:lintVitalAnalyzeRelease",
            "-x", ":app:lintVitalReportRelease",
            "-x", ":app:lintVitalRelease"
        )
    }
    & .\gradlew.bat @arguments
    if ($LASTEXITCODE -ne 0) { throw "The release build failed." }
}

function Find-ReleaseApk {
    $signed = Join-Path $projectDirectory "app\build\outputs\apk\release\app-release.apk"
    $unsigned = Join-Path $projectDirectory "app\build\outputs\apk\release\app-release-unsigned.apk"
    if (Test-Path -LiteralPath $signed) { return $signed }
    if (Test-Path -LiteralPath $unsigned) { return $unsigned }
    throw "No release APK was produced."
}

Push-Location $projectDirectory
try {
    if ($SkipLint) {
        Write-Warning "Release lint is being skipped explicitly. Do not distribute this build as a final release."
    }
    Invoke-ReleaseBuild
    $firstApk = Find-ReleaseApk
    $isUnsigned = [System.IO.Path]::GetFileName($firstApk) -like "*-unsigned.apk"
    if ($isUnsigned -and -not $AllowUnsigned) {
        throw "The release is unsigned. Configure signing or pass -AllowUnsigned for verification-only builds."
    }

    $buildFile = Get-Content -LiteralPath "app\build.gradle.kts" -Raw
    $versionName = [regex]::Match($buildFile, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
    $versionCode = [regex]::Match($buildFile, 'versionCode\s*=\s*(\d+)').Groups[1].Value
    if (-not $versionName -or -not $versionCode) {
        throw "The app version could not be read from app/build.gradle.kts."
    }

    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $suffix = if ($isUnsigned) { "-unsigned" } else { "" }
    $artifactName = "VeShinantam-$versionName-$versionCode-release$suffix.apk"
    $artifactPath = Join-Path $outputPath $artifactName
    Copy-Item -LiteralPath $firstApk -Destination $artifactPath -Force
    $firstHash = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash

    if ($VerifyReproducible) {
        Invoke-ReleaseBuild
        $secondHash = (Get-FileHash -LiteralPath (Find-ReleaseApk) -Algorithm SHA256).Hash
        if ($firstHash -ne $secondHash) {
            throw "Release builds were not reproducible: $firstHash != $secondHash"
        }
    }

    $checksumPath = "$artifactPath.sha256"
    "$firstHash *$artifactName" | Set-Content -LiteralPath $checksumPath -Encoding ascii
    [PSCustomObject]@{
        Artifact = $artifactPath
        ChecksumFile = $checksumPath
        SHA256 = $firstHash
        Signed = -not $isUnsigned
        EntitySyncEnabled = [bool]$EntitySyncEnabled
        ReproducibilityVerified = [bool]$VerifyReproducible
        LintSkipped = [bool]$SkipLint
    } | Format-List
} finally {
    Pop-Location
}
