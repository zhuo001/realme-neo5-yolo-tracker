# APK安装脚本 - 直接安装到连接的Realme Neo 5设备
# 运行前确保：1. 设备已连接并开启USB调试 2. 允许从未知来源安装

Write-Host "🚀 Realme Neo 5 YOLO Tracker APK 安装脚本" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""

# 检查设备连接
Write-Host "📱 检查设备连接..." -ForegroundColor Yellow
$devices = & .\platform-tools\adb.exe devices
Write-Host $devices

if ($devices -match "device$") {
    Write-Host "✅ 设备连接正常" -ForegroundColor Green
} else {
    Write-Host "❌ 未检测到设备或设备未授权" -ForegroundColor Red
    Write-Host "请确保：" -ForegroundColor Red
    Write-Host "1. USB调试已开启" -ForegroundColor Red
    Write-Host "2. 已授权此计算机进行调试" -ForegroundColor Red
    Write-Host "3. 设备正确连接" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📦 APK文件获取说明" -ForegroundColor Cyan
Write-Host "由于GitHub API限制，请手动下载APK：" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔗 下载地址：" -ForegroundColor White
Write-Host "https://github.com/zhuo001/realme-neo5-yolo-tracker/actions/runs/17378280440" -ForegroundColor Blue
Write-Host ""
Write-Host "📋 下载步骤：" -ForegroundColor White
Write-Host "1. 点击上方链接" -ForegroundColor White
Write-Host "2. 在页面底部找到 'Artifacts' 部分" -ForegroundColor White
Write-Host "3. 点击 'realme-neo5-yolo-debug-apk' 下载" -ForegroundColor White
Write-Host "4. 解压 zip 文件获得 APK" -ForegroundColor White
Write-Host "5. 将APK文件放在此目录下并重命名为 'app-debug.apk'" -ForegroundColor White
Write-Host ""

# 检查APK文件
$apkFile = "app-debug.apk"
if (Test-Path $apkFile) {
    Write-Host "✅ 找到APK文件: $apkFile" -ForegroundColor Green
    
    # 获取APK信息
    Write-Host ""
    Write-Host "📊 APK信息:" -ForegroundColor Yellow
    $fileSize = (Get-Item $apkFile).Length / 1MB
    Write-Host "文件大小: $([math]::Round($fileSize, 2)) MB" -ForegroundColor White
    
    # 卸载旧版本
    Write-Host ""
    Write-Host "🗑️ 卸载旧版本..." -ForegroundColor Yellow
    & .\platform-tools\adb.exe uninstall com.example.tracker 2>$null
    
    # 安装APK
    Write-Host ""
    Write-Host "⚡ 安装APK到设备..." -ForegroundColor Yellow
    $installResult = & .\platform-tools\adb.exe install -r $apkFile 2>&1
    
    if ($installResult -match "Success") {
        Write-Host "🎉 APK安装成功！" -ForegroundColor Green
        Write-Host ""
        Write-Host "🚀 启动应用测试:" -ForegroundColor Cyan
        Write-Host "1. 在设备上找到 'Tracker' 应用" -ForegroundColor White
        Write-Host "2. 点击启动" -ForegroundColor White
        Write-Host "3. 授予相机权限" -ForegroundColor White
        Write-Host "4. 观察YOLO检测效果" -ForegroundColor White
        Write-Host ""
        Write-Host "📊 预期效果:" -ForegroundColor Cyan
        Write-Host "✅ 相机预览正常显示" -ForegroundColor White
        Write-Host "✅ 显示 'NCNN模型加载成功' 提示" -ForegroundColor White
        Write-Host "✅ 右上角显示FPS计数" -ForegroundColor White
        Write-Host "✅ 检测到目标时显示绿色检测框" -ForegroundColor White
        Write-Host "✅ 推理时间显示 (通常5-20ms)" -ForegroundColor White
        Write-Host ""
        
        # 启动应用
        Write-Host "🎮 自动启动应用..." -ForegroundColor Yellow
        & .\platform-tools\adb.exe shell am start -n com.example.tracker/.MainActivity
        
        Write-Host ""
        Write-Host "📱 应用已启动！请在设备上查看YOLO检测效果" -ForegroundColor Green
        Write-Host ""
        
        # 显示日志
        Write-Host "📋 实时日志监控 (按Ctrl+C停止):" -ForegroundColor Cyan
        Write-Host "=====================================" -ForegroundColor Cyan
        & .\platform-tools\adb.exe logcat -c  # 清除旧日志
        & .\platform-tools\adb.exe logcat | Select-String -Pattern "(MainActivity|NCNN|YOLO|tracker)"
        
    } else {
        Write-Host "❌ APK安装失败:" -ForegroundColor Red
        Write-Host $installResult -ForegroundColor Red
        Write-Host ""
        Write-Host "🔧 故障排除:" -ForegroundColor Yellow
        Write-Host "1. 确保设备已开启 '允许安装未知来源应用'" -ForegroundColor White
        Write-Host "2. 检查设备存储空间是否足够" -ForegroundColor White
        Write-Host "3. 尝试重新连接设备" -ForegroundColor White
    }
    
} else {
    Write-Host "❌ 未找到APK文件: $apkFile" -ForegroundColor Red
    Write-Host ""
    Write-Host "📥 请按照上述步骤下载APK文件并放在此目录下" -ForegroundColor Yellow
    Write-Host "重命名为: app-debug.apk" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "📂 当前目录: $(Get-Location)" -ForegroundColor White
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "🏁 安装脚本执行完成" -ForegroundColor Green
