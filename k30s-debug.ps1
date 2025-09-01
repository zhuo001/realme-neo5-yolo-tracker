#!/usr/bin/env powershell
# K30S Ultra 快速调试脚本

param(
    [string]$Action = "deploy",
    [switch]$Monitor,
    [switch]$Debug
)

function Write-Step {
    param([string]$Message)
    Write-Host "`n🔸 $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Test-Device {
    Write-Step "检查 K30S Ultra 连接状态"
    $devices = adb devices
    if ($devices -match "device$") {
        $deviceInfo = adb shell getprop ro.product.model
        Write-Success "设备已连接: $deviceInfo"
        return $true
    } else {
        Write-Error "设备未连接或未授权USB调试"
        Write-Host "请检查："
        Write-Host "1. USB线连接正常"
        Write-Host "2. 开发者选项中已启用USB调试"
        Write-Host "3. 允许USB调试授权（如果有弹窗）"
        return $false
    }
}

function Deploy-App {
    Write-Step "构建并部署应用"
    
    # 检查是否有构建产物
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        Write-Step "未找到APK，开始构建..."
        # 这里可以添加gradle构建，如果环境允许
        Write-Error "请先通过Android Studio或gradle构建项目"
        return $false
    }
    
    # 安装APK
    Write-Step "安装APK到设备"
    $installResult = adb install -r $apkPath 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "APK安装成功"
    } else {
        Write-Error "APK安装失败: $installResult"
        return $false
    }
    
    # 授予相机权限
    Write-Step "授予必要权限"
    adb shell pm grant com.example.tracker android.permission.CAMERA
    Write-Success "相机权限已授予"
    
    # 启动应用
    Write-Step "启动应用"
    adb shell am start -n com.example.tracker/.MainActivity
    Start-Sleep -Seconds 2
    
    # 检查应用是否启动
    $appProcessId = adb shell pidof com.example.tracker
    if ($appProcessId) {
        Write-Success "应用启动成功 (PID: $appProcessId)"
        return $true
    } else {
        Write-Error "应用启动失败"
        return $false
    }
}

function Start-PerformanceMonitor {
    Write-Step "启动性能监控"
    Write-Host "监控应用性能... (Ctrl+C 停止)" -ForegroundColor Yellow
    
    # 清空日志缓冲区
    adb logcat -c
    
    # 监控关键指标
    adb logcat | Select-String -Pattern "(YoloTracker|FPS:|inference time:|CPU:|GPU:|Temp:|ERROR|WARN)"
}

function Initialize-Debug {
    Write-Step "准备Native调试环境"
    
    # 检查NDK环境变量
    if (-not $env:ANDROID_NDK_HOME) {
        Write-Error "未设置ANDROID_NDK_HOME环境变量"
        return $false
    }
    
    # 推送gdbserver
    $gdbserverPath = "$env:ANDROID_NDK_HOME\prebuilt\android-arm64\gdbserver"
    if (Test-Path $gdbserverPath) {
        Write-Step "推送gdbserver到设备"
        adb push $gdbserverPath /data/local/tmp/gdbserver
        adb shell chmod +x /data/local/tmp/gdbserver
        Write-Success "gdbserver已准备就绪"
    } else {
        Write-Error "gdbserver未找到: $gdbserverPath"
        return $false
    }
    
    # 获取应用PID
    $appProcessId = adb shell pidof com.example.tracker
    if ($appProcessId) {
        Write-Success "应用进程ID: $appProcessId"
        Write-Host "`n下一步："
        Write-Host "1. 在新的PowerShell窗口运行: adb shell /data/local/tmp/gdbserver :5039 --attach $appProcessId"
        Write-Host "2. 在VS Code中启动'Attach Native (gdb) arm64'调试配置"
        Write-Host "3. 输入PID: $appProcessId"
    } else {
        Write-Error "应用未运行，请先部署应用"
        return $false
    }
}

function Show-DeviceInfo {
    Write-Step "K30S Ultra 设备信息"
    
    $model = adb shell getprop ro.product.model
    $android = adb shell getprop ro.build.version.release
    $miui = adb shell getprop ro.miui.ui.version.name
    $chipset = adb shell getprop ro.board.platform
    
    Write-Host "设备型号: $model" -ForegroundColor Green
    Write-Host "Android版本: $android" -ForegroundColor Green
    Write-Host "MIUI版本: $miui" -ForegroundColor Green
    Write-Host "芯片平台: $chipset" -ForegroundColor Green
    
    Write-Step "温度状态"
    $temps = adb shell 'for f in /sys/class/thermal/thermal_zone*/temp; do echo "$(basename $(dirname $f)): $(cat $f)°C"; done'
    $temps | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
    
    Write-Step "内存状态"
    $meminfo = adb shell cat /proc/meminfo | Select-String -Pattern "(MemTotal|MemAvailable)"
    $meminfo | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
}

# 主流程
Write-Host "`n🚀 K30S Ultra YOLO Tracking 调试助手" -ForegroundColor Magenta
Write-Host "================================" -ForegroundColor Magenta

if (-not (Test-Device)) {
    exit 1
}

switch ($Action.ToLower()) {
    "info" {
        Show-DeviceInfo
    }
    "deploy" {
        if (Deploy-App) {
            Write-Success "`n🎉 部署完成！应用已在K30S Ultra上运行"
            if ($Monitor) {
                Start-PerformanceMonitor
            }
        }
    }
    "monitor" {
        Start-PerformanceMonitor
    }
    "debug" {
        Initialize-Debug
    }
    default {
        Write-Host "`n用法："
        Write-Host "  .\k30s-debug.ps1 deploy    # 部署应用"
        Write-Host "  .\k30s-debug.ps1 monitor   # 性能监控"
        Write-Host "  .\k30s-debug.ps1 debug     # 准备native调试"
        Write-Host "  .\k30s-debug.ps1 info      # 设备信息"
        Write-Host "`n参数："
        Write-Host "  -Monitor                   # 部署后自动开始监控"
    }
}
