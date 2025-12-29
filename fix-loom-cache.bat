@echo off
echo Fixing Gradle/Loom Cache Lock Issues...
echo.

echo Step 1: Stopping all Gradle daemons...
call gradlew.bat --stop
timeout /t 3 /nobreak >nul

echo Step 2: Killing any Java processes holding locks...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq *gradle*" 2>nul
taskkill /F /IM java.exe /FI "COMMANDLINE eq *gradle*" 2>nul
timeout /t 2 /nobreak >nul

echo Step 3: Clearing project cache...
if exist .gradle rmdir /s /q .gradle 2>nul
if exist build\loom-cache rmdir /s /q build\loom-cache 2>nul

echo Step 4: Attempting to clear global Loom cache...
echo    Note: If deletion fails due to long paths, you may need to:
echo    1. Use a tool like Long Path Tool or 
echo    2. Manually delete the folder via Explorer with long path support enabled
echo    3. Or reboot and delete after a clean start
echo.

echo Step 5: Starting fresh build...
echo    If this hangs for more than 2 minutes, press Ctrl+C
echo    Then try: Close all Java processes from Task Manager and reboot
echo.

call gradlew.bat clean --no-daemon

echo.
echo Done! If build succeeded, you can now run: gradlew.bat runClient
pause

