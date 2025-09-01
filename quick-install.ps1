# 快速安装脚本 - 直接下载并安装GitHub Actions构建的APK
param(
    [switch]$DownloadOnly,
    [switch]$InstallOnly
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Realme Neo 5 YOLO Tracker - 快速安装" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# GitHub API信息
$runId = "17378280440"
$artifactName = "realme-neo5-yolo-debug-apk"
$apkFileName = "app-debug.apk"

if (-not $InstallOnly) {
    Write-Host ""
    Write-Host "📥 准备下载APK..." -ForegroundColor Yellow
    Write-Host "由于GitHub API需要认证，请手动下载:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🔗 直接下载链接:" -ForegroundColor White
    Write-Host "https://github.com/zhuo001/realme-neo5-yolo-tracker/actions/runs/17378280440/artifacts/3898335826" -ForegroundColor Blue
    Write-Host ""
    Write-Host "📋 或通过Actions页面:" -ForegroundColor White
    Write-Host "https://github.com/zhuo001/realme-neo5-yolo-tracker/actions/runs/17378280440" -ForegroundColor Blue
    Write-Host ""
    Write-Host "⬇️ 下载后操作:" -ForegroundColor Yellow
    Write-Host "1. 解压下载的 zip 文件" -ForegroundColor White
    Write-Host "2. 将其中的 APK 文件重命名为 'app-debug.apk'" -ForegroundColor White
    Write-Host "3. 放在当前目录: $(Get-Location)" -ForegroundColor White
    Write-Host "4. 重新运行此脚本: .\install-apk.ps1 -InstallOnly" -ForegroundColor White
    
    if ($DownloadOnly) {
        Write-Host ""
        Write-Host "🛑 仅下载模式，脚本结束" -ForegroundColor Yellow
        return
    }
    
    Write-Host ""
    Write-Host "⏳ 等待APK文件..." -ForegroundColor Yellow
    Write-Host "请按任意键继续(确保APK已下载到当前目录)..." -ForegroundColor Cyan
    Read-Host
}

# 检查APK文件
if (-not (Test-Path $apkFileName)) {
    Write-Host "❌ 未找到APK文件: $apkFileName" -ForegroundColor Red
    Write-Host "请确保文件已下载并重命名为: $apkFileName" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ 找到APK文件" -ForegroundColor Green

# 检查设备连接
Write-Host ""
Write-Host "📱 检查设备连接..." -ForegroundColor Yellow
$adbPath = ".\platform-tools\adb.exe"
$devices = & $adbPath devices
# 过滤有效设备行
$deviceLines = $devices | Where-Object { $_ -match "\tdevice$" }
if ($deviceLines.Count -eq 0) {
    Write-Host "❌ 未检测到设备" -ForegroundColor Red
    Write-Host "请确保Realme Neo 5已连接并启用USB调试" -ForegroundColor Red
    exit 1
}

Write-Host "✅ 设备连接正常:" -ForegroundColor Green
Write-Host ($deviceLines -join "`n")

# 获取APK信息
$fileSize = (Get-Item $apkFileName).Length / 1MB
Write-Host "📊 APK大小: $([math]::Round($fileSize, 2)) MB" -ForegroundColor White

# 安装APK
Write-Host ""
Write-Host "🔄 卸载旧版本..." -ForegroundColor Yellow
& $adbPath uninstall com.example.tracker 2>$null | Out-Null

Write-Host "⚡ 安装新APK..." -ForegroundColor Yellow
$installResult = & $adbPath install -r $apkFileName 2>&1

if ($installResult -match "Success") {
    Write-Host "🎉 安装成功！" -ForegroundColor Green
    
    # 启动应用
    Write-Host ""
    Write-Host "🚀 启动应用..." -ForegroundColor Yellow
    & $adbPath shell am start -n com.example.tracker/.MainActivity
    
    Write-Host ""
    Write-Host "📱 应用已启动！请检查以下功能:" -ForegroundColor Cyan
    Write-Host "✅ 相机预览" -ForegroundColor White
    Write-Host "✅ NCNN加载提示" -ForegroundColor White  
    Write-Host "✅ FPS显示" -ForegroundColor White
    Write-Host "✅ YOLO检测框" -ForegroundColor White
    
    Write-Host ""
    Write-Host "📋 查看实时日志 (按Ctrl+C停止):" -ForegroundColor Cyan
    Write-Host "===============================" -ForegroundColor Cyan
    
    # 清除旧日志并显示实时日志
    & $adbPath logcat -c
    & $adbPath logcat | Where-Object { $_ -match "(MainActivity|NCNN|YOLO|tracker|YoloDetector|NativeBridge)" }
    
} else {
    Write-Host "❌ 安装失败:" -ForegroundColor Red
    Write-Host $installResult -ForegroundColor Red
}
