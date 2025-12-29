# Fix Gradle/Loom Cache Lock Issues
# Run this script if Gradle build hangs due to cache locks

Write-Host "Stopping all Gradle daemons..." -ForegroundColor Yellow
.\gradlew.bat --stop

Write-Host "Killing Java processes that might be holding locks..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*jdk*" } | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Clearing local project cache..." -ForegroundColor Yellow
Remove-Item -Path ".\.gradle" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\.gradle\loom-cache" -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Clearing global Loom cache locks..." -ForegroundColor Yellow
$loomCache = "$env:USERPROFILE\scoop\apps\gradle\current\.gradle\caches\fabric-loom"
if (Test-Path $loomCache) {
    Get-ChildItem -Path $loomCache -Filter "*.lock" -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue
    Write-Host "  Found and removed lock files in global cache" -ForegroundColor Green
}

Write-Host "Waiting 3 seconds for locks to clear..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`nAttempting clean build (this may take a moment)..." -ForegroundColor Yellow
Write-Host "If this still hangs, you may need to:" -ForegroundColor Cyan
Write-Host "  1. Close all Java processes manually from Task Manager" -ForegroundColor Cyan
Write-Host "  2. Reboot your computer" -ForegroundColor Cyan
Write-Host "  3. Delete the entire fabric-loom cache folder manually" -ForegroundColor Cyan
Write-Host "`nPress Ctrl+C if it hangs for more than 2 minutes..." -ForegroundColor Red
Write-Host ""

.\gradlew.bat clean --no-daemon

