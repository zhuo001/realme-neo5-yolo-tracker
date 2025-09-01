# 快速 ADB 工具下载和设备连接助手

Write-Host "🚀 设置轻量级Android调试环境..." -ForegroundColor Magenta

# 创建工具目录
$toolsDir = ".\android-tools"
if (-not (Test-Path $toolsDir)) {
    New-Item -Path $toolsDir -ItemType Directory | Out-Null
    Write-Host "✅ 创建工具目录: $toolsDir" -ForegroundColor Green
}

# 检查是否已有ADB
$adbPath = Join-Path $toolsDir "adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Host "📥 需要下载ADB工具..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "请手动下载独立ADB工具包：" -ForegroundColor Blue
    Write-Host "1. 访问: https://developer.android.com/studio/releases/platform-tools"
    Write-Host "2. 下载 'SDK Platform Tools for Windows'"
    Write-Host "3. 解压到当前目录的 android-tools 文件夹"
    Write-Host ""
    Write-Host "或者使用这个直链下载:" -ForegroundColor Green
    Write-Host "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
    Write-Host ""
    Write-Host "下载完成后重新运行此脚本"
    return
}

# ADB已存在，开始设备检查
Write-Host "✅ 找到ADB工具" -ForegroundColor Green

# 设置环境变量
$env:PATH = "$((Get-Location).Path)\$toolsDir;$env:PATH"

Write-Host "🔸 检查设备连接..." -ForegroundColor Cyan

try {
    # 启动ADB服务
    & $adbPath start-server
    Start-Sleep -Seconds 2
    
    # 检查设备列表
    $devices = & $adbPath devices
    Write-Host "设备列表:" -ForegroundColor Yellow
    Write-Host $devices
    
    if ($devices -match "device$") {
        Write-Host ""
        Write-Host "✅ 设备已成功连接！" -ForegroundColor Green
        
        # 获取设备详细信息
        Write-Host "📱 设备信息:" -ForegroundColor Magenta
        
        $brand = & $adbPath shell getprop ro.product.brand
        $model = & $adbPath shell getprop ro.product.model  
        $android = & $adbPath shell getprop ro.build.version.release
        $sdk = & $adbPath shell getprop ro.build.version.sdk
        $arch = & $adbPath shell getprop ro.product.cpu.abi
        
        Write-Host "品牌: $brand" -ForegroundColor White
        Write-Host "型号: $model" -ForegroundColor White
        Write-Host "Android: $android (API $sdk)" -ForegroundColor White
        Write-Host "架构: $arch" -ForegroundColor White
        
        # 检查开发者选项
        Write-Host ""
        Write-Host "🔧 开发者选项检查:" -ForegroundColor Cyan
        
        $devOptions = & $adbPath shell settings get global development_settings_enabled
        if ($devOptions -eq "1") {
            Write-Host "✅ 开发者选项已启用" -ForegroundColor Green
        } else {
            Write-Host "⚠️ 开发者选项未启用" -ForegroundColor Yellow
        }
        
        # 检查USB调试
        $usbDebug = & $adbPath shell settings get global adb_enabled  
        if ($usbDebug -eq "1") {
            Write-Host "✅ USB调试已启用" -ForegroundColor Green
        } else {
            Write-Host "⚠️ USB调试未启用" -ForegroundColor Yellow
        }
        
        Write-Host ""
        Write-Host "🎯 准备就绪，可以开始以下操作:" -ForegroundColor Green
        Write-Host "1. 构建项目: gradlew assembleDebug"
        Write-Host "2. 部署应用: $adbPath install -r app-debug.apk"
        Write-Host "3. 启动调试: $adbPath logcat"
        Write-Host "4. 运行脚本: .\k30s-debug.ps1 (需要修正ADB路径)"
        
    } elseif ($devices -match "unauthorized") {
        Write-Host "⚠️ 设备未授权USB调试" -ForegroundColor Yellow
        Write-Host "请在手机上点击'允许USB调试'，然后重新运行脚本"
        
    } else {
        Write-Host "❌ 未检测到设备" -ForegroundColor Red
        Write-Host ""
        Write-Host "排查步骤:" -ForegroundColor Yellow
        Write-Host "1. 确认USB线连接正常（建议使用数据线，不是充电线）"
        Write-Host "2. 手机设置 → 关于手机 → 连续点击版本号7次启用开发者选项"
        Write-Host "3. 开发者选项 → 启用USB调试"
        Write-Host "4. 连接电脑时选择'传输文件'模式"
        Write-Host "5. 允许USB调试授权弹窗"
    }
    
} catch {
    Write-Host "❌ ADB执行失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "💡 提示: 如需在其他窗口使用ADB，请运行:" -ForegroundColor Blue
Write-Host "`$env:PATH = `"$((Get-Location).Path)\$toolsDir;`$env:PATH`""
