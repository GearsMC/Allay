@echo off
TITLE Allay server software for Minecraft: Bedrock Edition
cd /d %~dp0

set "JAVA_BIN="

if exist "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe"
)

if "%JAVA_BIN%"=="" (
    for /d %%D in ("C:\Program Files\Microsoft\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA_BIN=%%D\bin\java.exe"
    )
)

if "%JAVA_BIN%"=="" (
    echo Java 21 bulunamadi. Kurulum: winget install --id Microsoft.OpenJDK.21 -e
    pause
    exit /b 1
)

set "JAR="
for /f "delims=" %%F in ('dir /b /o-d "server\build\libs\allay-server-*-shaded.jar" 2^>nul') do (
    if not defined JAR set "JAR=%~dp0server\build\libs\%%F"
)

if "%JAR%"=="" (
    echo Shaded JAR bulunamadi. Once derleyin: gradlew.bat :server:shadowJar
    pause
    exit /b 1
)

if not exist ".run" mkdir ".run"
cd /d "%~dp0.run"

"%JAVA_BIN%" -Dfile.encoding=UTF-8 -Xms1G -Xmx4G -jar "%JAR%" %* || pause
