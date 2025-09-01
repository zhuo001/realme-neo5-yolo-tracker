# Android 开发环境配置助手

Write-Host "🔍 检查Android开发环境..." -ForegroundColor Cyan

# 检查常见的Android SDK位置
$possibleSdkPaths = @(
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk",
    "C:\Android\Sdk",
    "D:\Android\Sdk",
    "C:\Program Files\Android\Android Studio\sdk",
    "C:\Program Files (x86)\Android\Android Studio\sdk"
)

$sdkPath = $null
foreach ($path in $possibleSdkPaths) {
    if (Test-Path $path) {
        $sdkPath = $path
        Write-Host "✅ 找到Android SDK: $path" -ForegroundColor Green
        break
    }
}

if (-not $sdkPath) {
    Write-Host "❌ 未找到Android SDK" -ForegroundColor Red
    Write-Host ""
    Write-Host "请选择以下选项：" -ForegroundColor Yellow
    Write-Host "1. 安装Android Studio (推荐)"
    Write-Host "2. 下载Android SDK命令行工具"
    Write-Host "3. 手动指定SDK路径"
    Write-Host ""
    Write-Host "Android Studio下载链接:" -ForegroundColor Blue
    Write-Host "https://developer.android.com/studio"
    exit 1
}

# 设置环境变量
$env:ANDROID_HOME = $sdkPath
$env:ANDROID_SDK_ROOT = $sdkPath
$adbPath = Join-Path $sdkPath "platform-tools\adb.exe"

if (Test-Path $adbPath) {
    Write-Host "✅ 找到ADB: $adbPath" -ForegroundColor Green
    
    # 添加到当前会话的PATH
    $env:PATH += ";$(Join-Path $sdkPath 'platform-tools')"
    
    Write-Host "🔸 检查设备连接..." -ForegroundColor Cyan
    $devices = & $adbPath devices
    Write-Host $devices
    
    if ($devices -match "device$") {
        Write-Host "✅ 设备已连接" -ForegroundColor Green
        
        # 获取设备信息
        $model = & $adbPath shell getprop ro.product.model
        $android = & $adbPath shell getprop ro.build.version.release
        $brand = & $adbPath shell getprop ro.product.brand
        
        Write-Host ""
        Write-Host "📱 设备信息:" -ForegroundColor Magenta
        Write-Host "品牌: $brand"
        Write-Host "型号: $model" 
        Write-Host "Android版本: $android"
        
        Write-Host ""
        Write-Host "🎯 下一步可以执行:" -ForegroundColor Green
        Write-Host "1. 构建项目: .\gradlew assembleDebug"
        Write-Host "2. 安装应用: $adbPath install -r app\build\outputs\apk\debug\app-debug.apk"
        Write-Host "3. 运行调试脚本: .\k30s-debug.ps1 deploy"
        
    } else {
        Write-Host "⚠️ 设备未连接或未授权" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "请确认："
        Write-Host "1. USB线连接正常"
        Write-Host "2. 手机开启开发者选项"
        Write-Host "3. 启用USB调试"
        Write-Host "4. 允许电脑调试授权（如有弹窗）"
    }
    
} else {
    Write-Host "❌ ADB未找到在: $adbPath" -ForegroundColor Red
}

Write-Host ""
Write-Host "💡 如需永久设置环境变量，请运行:" -ForegroundColor Blue
Write-Host "[Environment]::SetEnvironmentVariable('ANDROID_HOME', '$sdkPath', 'User')"
Write-Host "[Environment]::SetEnvironmentVariable('PATH', `"`$env:PATH;$sdkPath\platform-tools`", 'User')"
