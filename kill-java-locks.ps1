# Kill all Java processes that might be holding Gradle locks
# Run this before building if you get lock errors

Write-Host "Killing all Java processes..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Stopping Gradle daemons..." -ForegroundColor Yellow
& "$PSScriptRoot\gradlew.bat" --stop 2>&1 | Out-Null

Write-Host "Removing lock files..." -ForegroundColor Yellow
$loomPath = "$env:USERPROFILE\scoop\apps\gradle\current\.gradle\caches\fabric-loom"
if (Test-Path $loomPath) {
    Get-ChildItem -Path $loomPath -Recurse -File -Filter "*.lock" | Remove-Item -Force -ErrorAction SilentlyContinue
    Write-Host "  Lock files removed" -ForegroundColor Green
} else {
    Write-Host "  fabric-loom cache not found" -ForegroundColor Yellow
}

Write-Host "Waiting 3 seconds..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`nDone! You can now try building again." -ForegroundColor Green
Write-Host "Run: .\gradlew.bat clean" -ForegroundColor Cyan

