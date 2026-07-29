[CmdletBinding(PositionalBinding = $false)]
param (
    [switch]$SkipBuild = $false,
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

$runDir = Join-Path $PSScriptRoot ".run"
$pluginsDir = Join-Path $runDir "plugins"
$pluginProjectDir = Join-Path $pluginsDir "SkyblockA"

if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "=== [1/3] AllayMC derleniyor ==="
    & .\gradlew.bat :api:publishToMavenLocal :server:shadowJar --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host "AllayMC derlemesi basarisiz."
        pause
        exit $LASTEXITCODE
    }

    Write-Host ""
    Write-Host "=== [2/3] GearsCore plugin derleniyor ==="
    if (-not (Test-Path (Join-Path $pluginProjectDir "gradlew.bat"))) {
        Write-Host "Plugin projesi bulunamadi: $pluginProjectDir"
        pause
        exit 1
    }

    Push-Location -LiteralPath $pluginProjectDir
    try {
        & .\gradlew.bat jar --quiet
        if ($LASTEXITCODE -ne 0) {
            Write-Host "GearsCore derlemesi basarisiz."
            pause
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }

    $pluginJar = Get-ChildItem (Join-Path $pluginProjectDir "build\libs\GearsCore-*.jar") -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $pluginJar) {
        Write-Host "GearsCore JAR bulunamadi."
        pause
        exit 1
    }

    if (-not (Test-Path $pluginsDir)) {
        New-Item -ItemType Directory -Path $pluginsDir | Out-Null
    }

    $pluginDest = Join-Path $pluginsDir "GearsCore-1.0.0.jar"
    Copy-Item -LiteralPath $pluginJar.FullName -Destination $pluginDest -Force
    Write-Host "Plugin guncellendi: $pluginDest"
}

$jar = Get-ChildItem "server\build\libs\allay-server-*-shaded.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    Write-Host "Shaded JAR bulunamadi. Once derleyin (SkipBuild kullanmadan start edin)."
    pause
    exit 1
}

if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

function Start-AllayServer {
    Write-Host ""
    Write-Host "=== Sunucu baslatiliyor ==="
    Write-Host "Java: $env:JAVA_HOME"
    Write-Host "JAR : $($jar.FullName)"
    Write-Host "CWD : $runDir"

    $ensurePostgres = Join-Path $PSScriptRoot "ensure-postgres.ps1"
    if (Test-Path $ensurePostgres) {
        & powershell -NoProfile -ExecutionPolicy Bypass -File $ensurePostgres
        if ($LASTEXITCODE -ne 0) {
            Write-Host "PostgreSQL baslatilamadi."
            pause
            exit 1
        }
    }

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
