# 简化设备状态检查脚本

Write-Host "Device Status Check" -ForegroundColor Cyan

# Check current device status
$devices = adb devices
Write-Host "Current device status:"
Write-Host $devices

if ($devices -match "device$") {
    Write-Host "SUCCESS: Device is authorized and ready!" -ForegroundColor Green
    
    # Get device information
    $brand = adb shell getprop ro.product.brand
    $model = adb shell getprop ro.product.model
    $android = adb shell getprop ro.build.version.release
    
    Write-Host ""
    Write-Host "Device Info:" -ForegroundColor Yellow
    Write-Host "Brand: $brand"
    Write-Host "Model: $model"
    Write-Host "Android: $android"
    
    Write-Host ""
    Write-Host "Ready for next steps:" -ForegroundColor Green
    Write-Host "1. Build project (need Android SDK/Studio)"
    Write-Host "2. Install APK: adb install -r app-debug.apk"
    Write-Host "3. Start monitoring: adb logcat"
    
} elseif ($devices -match "unauthorized") {
    Write-Host "WAITING: Device needs USB debugging authorization" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please authorize USB debugging on your phone:"
    Write-Host "- Look for USB debugging dialog"
    Write-Host "- Check 'Always allow from this computer'"
    Write-Host "- Tap 'Allow' or 'OK'"
    
} else {
    Write-Host "ERROR: No device found" -ForegroundColor Red
    Write-Host "Please check USB connection and enable developer options"
}

Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Blue
Write-Host "adb devices        # Check device status"
Write-Host "adb logcat         # View logs"
Write-Host "adb shell          # Access device shell"
