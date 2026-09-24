param(
    [switch]$AllowUnsigned,
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

function Get-ApkProtectedContentHash([string]$ApkPath) {
    $bytes = [System.IO.File]::ReadAllBytes($ApkPath)
    $minimumEocdLength = 22
    $maximumCommentLength = 65535
    $eocdStart = [Math]::Max(0, $bytes.Length - $minimumEocdLength - $maximumCommentLength)
    $eocdOffset = -1
    for ($index = $bytes.Length - $minimumEocdLength; $index -ge $eocdStart; $index--) {
        if ($bytes[$index] -eq 0x50 -and $bytes[$index + 1] -eq 0x4b -and
            $bytes[$index + 2] -eq 0x05 -and $bytes[$index + 3] -eq 0x06) {
            $eocdOffset = $index
            break
        }
    }
    if ($eocdOffset -lt 0) { throw "APK end-of-central-directory record was not found: $ApkPath" }

    $centralDirectoryOffset = [BitConverter]::ToUInt32($bytes, $eocdOffset + 16)
    if ($centralDirectoryOffset -lt 24) { throw "APK signing block footer is missing: $ApkPath" }
    $footerOffset = [int64]$centralDirectoryOffset - 24
    $magic = [Text.Encoding]::ASCII.GetString($bytes, [int]$footerOffset + 8, 16)
    if ($magic -ne "APK Sig Block 42") { throw "APK v2+ signing block was not found: $ApkPath" }

    $signingBlockSize = [BitConverter]::ToUInt64($bytes, [int]$footerOffset)
    $signingBlockStart = [int64]$centralDirectoryOffset - [int64]$signingBlockSize - 8
    if ($signingBlockStart -lt 0) { throw "APK signing block has an invalid size: $ApkPath" }

    $hash = [Security.Cryptography.IncrementalHash]::CreateHash(
        [Security.Cryptography.HashAlgorithmName]::SHA256
    )
    try {
        $hash.AppendData($bytes, 0, [int]$signingBlockStart)
        $hash.AppendData(
            $bytes,
            [int]$centralDirectoryOffset,
            $bytes.Length - [int]$centralDirectoryOffset
        )
        return [Convert]::ToHexString($hash.GetHashAndReset())
    } finally {
        $hash.Dispose()
    }
}

function Get-VerifiedSignerCertificateHash([string]$ApkPath) {
    $sdkRoot = if ($env:ANDROID_HOME) {
        $env:ANDROID_HOME
    } else {
        Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
    $apksigner = Get-ChildItem -Path (Join-Path $sdkRoot "build-tools\*\apksigner.bat") -File |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if (-not $apksigner) { throw "Android apksigner was not found below $sdkRoot." }

    $verification = & $apksigner.FullName verify --verbose --print-certs $ApkPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed: $ApkPath`n$($verification -join [Environment]::NewLine)"
    }
    $certificateLine = $verification |
        Where-Object { $_ -match 'certificate SHA-256 digest: ([0-9a-fA-F]+)$' } |
        Select-Object -First 1
    if (-not $certificateLine) { throw "APK signer certificate digest was not reported: $ApkPath" }
    [void]($certificateLine -match 'certificate SHA-256 digest: ([0-9a-fA-F]+)$')
    return $Matches[1].ToUpperInvariant()
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
        $secondApk = Find-ReleaseApk
        $secondHash = (Get-FileHash -LiteralPath $secondApk -Algorithm SHA256).Hash
        if ($firstHash -ne $secondHash) {
            if ($isUnsigned) {
                throw "Unsigned release builds were not reproducible: $firstHash != $secondHash"
            }

            # APK Signature Scheme v2 may use randomized RSA-PSS signatures.
            # Compare every protected byte outside the signing block, then
            # require both APKs to verify with the same certificate.
            $firstContentHash = Get-ApkProtectedContentHash $artifactPath
            $secondContentHash = Get-ApkProtectedContentHash $secondApk
            if ($firstContentHash -ne $secondContentHash) {
                throw "Signed release payloads were not reproducible: $firstContentHash != $secondContentHash"
            }
            $firstCertificateHash = Get-VerifiedSignerCertificateHash $artifactPath
            $secondCertificateHash = Get-VerifiedSignerCertificateHash $secondApk
            if ($firstCertificateHash -ne $secondCertificateHash) {
                throw "Release builds used different signing certificates: $firstCertificateHash != $secondCertificateHash"
            }
        }
    }

    $checksumPath = "$artifactPath.sha256"
    "$firstHash *$artifactName" | Set-Content -LiteralPath $checksumPath -Encoding ascii
    [PSCustomObject]@{
        Artifact = $artifactPath
        ChecksumFile = $checksumPath
        SHA256 = $firstHash
        Signed = -not $isUnsigned
        ReproducibilityVerified = [bool]$VerifyReproducible
        ReproducibilityScope = if (-not $VerifyReproducible) {
            "Not requested"
        } elseif ($isUnsigned -or $firstHash -eq $secondHash) {
            "Whole APK"
        } else {
            "Protected APK content; randomized v2 signing block excluded"
        }
        LintSkipped = [bool]$SkipLint
    } | Format-List
} finally {
    Pop-Location
}
