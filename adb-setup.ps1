# ADB Setup and Device Check Script

Write-Host "Setting up Android Debug Bridge..." -ForegroundColor Cyan

# Create tools directory
$toolsDir = "android-tools"
if (-not (Test-Path $toolsDir)) {
    New-Item -Path $toolsDir -ItemType Directory | Out-Null
    Write-Host "Created tools directory: $toolsDir" -ForegroundColor Green
}

# Check if ADB exists
$adbPath = Join-Path $toolsDir "adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Host "ADB not found. Please download platform tools:" -ForegroundColor Yellow
    Write-Host "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
    Write-Host "Extract to the android-tools folder and run this script again."
    return
}

Write-Host "ADB found. Checking device..." -ForegroundColor Green

# Set PATH for this session
$env:PATH = "$PWD\$toolsDir;$env:PATH"

try {
    # Start ADB server
    & $adbPath start-server | Out-Null
    Start-Sleep -Seconds 2
    
    # Check devices
    $devices = & $adbPath devices
    Write-Host "Device List:"
    Write-Host $devices
    
    if ($devices -match "device$") {
        Write-Host ""
        Write-Host "SUCCESS: Device is connected!" -ForegroundColor Green
        
        # Get device info
        $brand = & $adbPath shell getprop ro.product.brand
        $model = & $adbPath shell getprop ro.product.model
        $android = & $adbPath shell getprop ro.build.version.release
        
        Write-Host "Brand: $brand"
        Write-Host "Model: $model" 
        Write-Host "Android: $android"
        
        Write-Host ""
        Write-Host "Ready to proceed with:"
        Write-Host "1. Build project"
        Write-Host "2. Install APK" 
        Write-Host "3. Start debugging"
        
    } else {
        Write-Host "Device not found or unauthorized" -ForegroundColor Red
        Write-Host "Please check USB connection and enable USB debugging"
    }
    
} catch {
    Write-Host "ADB execution failed: $_" -ForegroundColor Red
}
