# Fixing Gradle/Loom Cache Lock Issues

If your Gradle build is hanging at "CONFIGURING" with lock file errors, follow these steps:

## Quick Fix (Automated)

1. **Run the fix script:**
   ```
   fix-loom-cache.bat
   ```

## Manual Fix (If automated doesn't work)

1. **Stop all Gradle processes:**
   - Open Task Manager (Ctrl+Shift+Esc)
   - End all `java.exe` processes
   - Or run: `.\gradlew.bat --stop`

2. **Delete cache folders:**
   - Delete `.gradle` folder in your project directory
   - Delete `build\loom-cache` folder if it exists

3. **Clear global Loom cache:**
   - Location: `C:\Users\Ritro\scoop\apps\gradle\current\.gradle\caches\fabric-loom`
   - **Note:** Windows path length limits may prevent deletion
   - Options:
     - Use 7-Zip or WinRAR to delete long paths
     - Enable Windows long path support (Group Policy)
     - Or just delete the entire `fabric-loom` folder

4. **Rebuild:**
   ```
   .\gradlew.bat clean
   .\gradlew.bat build
   ```

## If Build Still Hangs

1. Close ALL Java processes from Task Manager
2. Reboot your computer
3. Delete the `fabric-loom` cache folder manually
4. Try building again

The hang usually happens when:
- Previous build was interrupted (Ctrl+C)
- Multiple Gradle processes are running
- Cache lock files are corrupted

