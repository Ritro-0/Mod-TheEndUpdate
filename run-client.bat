@echo off
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

REM Change to this script's directory (project root)
cd /d "%~dp0"

REM Optional: quick Java version check (expects 21)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%v
set JVER=!JVER:~1,-1!
echo Using Java version: !JVER!

echo.
echo Launching Fabric dev client (this may take a while the first time)...
echo.

call gradlew.bat --no-daemon runClient

echo.
echo Run finished. Press any key to close.
pause >nul


