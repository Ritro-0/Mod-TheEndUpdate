# Script to delete fabric-loom cache using robocopy to handle long paths
$loomPath = "$env:USERPROFILE\scoop\apps\gradle\current\.gradle\caches\fabric-loom"

if (-not (Test-Path $loomPath)) {
    Write-Host "fabric-loom cache folder not found at: $loomPath" -ForegroundColor Yellow
    exit 0
}

Write-Host "Deleting fabric-loom cache folder..." -ForegroundColor Yellow
Write-Host "Using robocopy workaround for long Windows paths..." -ForegroundColor Cyan

# Stop all Java processes first
Write-Host "Stopping Java processes..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Create an empty temp folder
$tempEmpty = "$env:TEMP\empty_folder_$(Get-Random)"
New-Item -ItemType Directory -Path $tempEmpty -Force | Out-Null

try {
    # Use robocopy to mirror empty folder (effectively deleting everything)
    $null = robocopy $tempEmpty $loomPath /MIR /R:0 /W:0 /NP /NFL /NDL /NJH /NJS
    
    # Remove the now-empty folder
    Start-Sleep -Seconds 1
    Remove-Item -Path $loomPath -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $tempEmpty -Force -ErrorAction SilentlyContinue
    
    if (-not (Test-Path $loomPath)) {
        Write-Host "Successfully deleted fabric-loom cache!" -ForegroundColor Green
    } else {
        Write-Host "Partial deletion - some files may remain. Trying standard method..." -ForegroundColor Yellow
        Remove-Item -Path $loomPath -Recurse -Force -ErrorAction SilentlyContinue
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "`nYou may need to:" -ForegroundColor Yellow
    Write-Host "1. Enable Windows long path support (requires admin)" -ForegroundColor Cyan
    Write-Host "2. Delete the folder manually using 7-Zip or WinRAR" -ForegroundColor Cyan
    Write-Host "3. Or reboot and delete it before starting any builds" -ForegroundColor Cyan
} finally {
    # Clean up temp folder
    Remove-Item -Path $tempEmpty -Force -ErrorAction SilentlyContinue
}

