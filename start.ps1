[CmdletBinding(PositionalBinding = $false)]
param (
    [switch]$Build = $false,
    [switch]$Loop = $false,
    [string]$JavaHome = "",
    [string][Parameter(ValueFromRemainingArguments)]$ExtraArgs
)

Set-Location -LiteralPath $PSScriptRoot

function Resolve-Java21 {
    param([string]$Preferred)

    if ($Preferred -ne "" -and (Test-Path (Join-Path $Preferred "bin\java.exe"))) {
        return $Preferred
    }

    $candidates = @(
        "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot",
        "C:\Program Files\Microsoft\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Java\jdk-21*"
    )

    foreach ($pattern in $candidates) {
        $match = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($match -and (Test-Path (Join-Path $match.FullName "bin\java.exe"))) {
            return $match.FullName
        }
    }

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $version = & (Join-Path $env:JAVA_HOME "bin\java.exe") -version 2>&1 | Out-String
        if ($version -match 'version "21\.') {
            return $env:JAVA_HOME
        }
    }

    return $null
}

$resolvedJavaHome = Resolve-Java21 -Preferred $JavaHome
if (-not $resolvedJavaHome) {
    Write-Host "Java 21 bulunamadi. Microsoft OpenJDK 21 kurulu olmali."
    Write-Host "Kurulum: winget install --id Microsoft.OpenJDK.21 -e"
    pause
    exit 1
}

$env:JAVA_HOME = $resolvedJavaHome
$env:PATH = "$env:JAVA_HOME\bin;" + ($env:PATH -replace [regex]::Escape("$env:JAVA_HOME\bin;"), "")
$java = Join-Path $env:JAVA_HOME "bin\java.exe"

if ($Build) {
    Write-Host "Allay derleniyor..."
    & .\gradlew.bat :server:shadowJar --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Derleme basarisiz."
        pause
        exit $LASTEXITCODE
    }
}

$jar = Get-ChildItem "server\build\libs\allay-server-*-shaded.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    Write-Host "Shaded JAR bulunamadi. Once derleyin:"
    Write-Host "  .\start.ps1 -Build"
    Write-Host "veya"
    Write-Host "  .\gradlew.bat :server:shadowJar"
    pause
    exit 1
}

$runDir = Join-Path $PSScriptRoot ".run"
if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

function Start-AllayServer {
    Write-Host "Java: $env:JAVA_HOME"
    Write-Host "JAR : $($jar.FullName)"
    Write-Host "CWD : $runDir"
    Set-Location -LiteralPath $runDir
    $jvmArgs = @(
        "-Dfile.encoding=UTF-8",
        "-Xms1G",
        "-Xmx4G",
        "-jar",
        $jar.FullName
    )
    if ($ExtraArgs) {
        $jvmArgs += $ExtraArgs
    }
    & $java @jvmArgs
    Set-Location -LiteralPath $PSScriptRoot
}

$loops = 0
Start-AllayServer

while ($Loop) {
    if ($loops -ne 0) {
        Write-Host ("Yeniden baslatildi: {0}" -f $loops)
    }
    $loops++
    Write-Host "Donguden cikmak icin CTRL+C. 5 saniye sonra yeniden baslar..."
    Start-Sleep 5
    Start-AllayServer
}
