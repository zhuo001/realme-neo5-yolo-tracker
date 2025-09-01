# 设备授权监控和自动部署脚本

Write-Host "🚀 K30S Ultra 自动部署助手" -ForegroundColor Magenta
Write-Host "=====================================" -ForegroundColor Magenta

function Test-DeviceStatus {
    $devices = adb devices 2>&1
    if ($devices -match "device$") {
        return "authorized"
    } elseif ($devices -match "unauthorized") {
        return "unauthorized" 
    } else {
        return "disconnected"
    }
}

function Get-DeviceInfo {
    try {
        $brand = adb shell getprop ro.product.brand 2>&1
        $model = adb shell getprop ro.product.model 2>&1
        $android = adb shell getprop ro.build.version.release 2>&1
        $sdk = adb shell getprop ro.build.version.sdk 2>&1
        
        Write-Host "📱 设备信息:" -ForegroundColor Green
        Write-Host "品牌: $brand"
        Write-Host "型号: $model"
        Write-Host "Android: $android (API $sdk)"
        
        return $true
    } catch {
        Write-Host "❌ 无法获取设备信息: $_" -ForegroundColor Red
        return $false
    }
}

function Deploy-Application {
    Write-Host ""
    Write-Host "🔨 开始应用部署流程..." -ForegroundColor Cyan
    
    # 检查是否有APK文件
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        Write-Host "⚠️ 未找到APK文件，需要先构建项目" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "构建步骤："
        Write-Host "1. 如果有Android Studio: 打开项目 → Build → Build APK"
        Write-Host "2. 或者运行: gradlew assembleDebug (需要Java环境)"
        Write-Host "3. APK将生成在: $apkPath"
        return $false
    }
    
    # 安装APK
    Write-Host "📦 安装APK..." -ForegroundColor Cyan
    $installResult = adb install -r $apkPath 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ APK安装成功" -ForegroundColor Green
    } else {
        Write-Host "❌ APK安装失败: $installResult" -ForegroundColor Red
        return $false
    }
    
    # 授予权限
    Write-Host "🔐 授予必要权限..." -ForegroundColor Cyan
    adb shell pm grant com.example.tracker android.permission.CAMERA 2>&1 | Out-Null
    adb shell pm grant com.example.tracker android.permission.WRITE_EXTERNAL_STORAGE 2>&1 | Out-Null
    Write-Host "✅ 权限已授予" -ForegroundColor Green
    
    # 启动应用
    Write-Host "🚀 启动应用..." -ForegroundColor Cyan
    adb shell am start -n com.example.tracker/.MainActivity 2>&1 | Out-Null
    Start-Sleep -Seconds 3
    
    # 检查应用是否启动
    $processId = adb shell pidof com.example.tracker 2>&1
    if ($processId -and $processId -match "\d+") {
        Write-Host "✅ 应用启动成功 (PID: $processId)" -ForegroundColor Green
        return $true
    } else {
        Write-Host "⚠️ 应用可能未启动或已崩溃" -ForegroundColor Yellow
        return $false
    }
}

function Start-LogMonitoring {
    Write-Host ""
    Write-Host "📊 开始日志监控..." -ForegroundColor Cyan
    Write-Host "按 Ctrl+C 停止监控" -ForegroundColor Yellow
    Write-Host ""
    
    # 清空日志缓冲区
    adb logcat -c
    
    # 监控关键日志
    adb logcat | Select-String -Pattern "(YoloTracker|FPS|inference|ERROR|FATAL|AndroidRuntime)"
}

# 主流程开始
Write-Host ""
Write-Host "🔍 当前设备状态..." -ForegroundColor Cyan
$status = Test-DeviceStatus

switch ($status) {
    "authorized" {
        Write-Host "✅ 设备已授权，可以开始部署！" -ForegroundColor Green
        
        if (Get-DeviceInfo) {
            Write-Host ""
            $deploy = Read-Host "是否立即部署应用？(y/n)"
            if ($deploy -eq "y" -or $deploy -eq "Y") {
                if (Deploy-Application) {
                    Write-Host ""
                    $monitor = Read-Host "是否开始日志监控？(y/n)"
                    if ($monitor -eq "y" -or $monitor -eq "Y") {
                        Start-LogMonitoring
                    }
                }
            }
        }
    }
    
    "unauthorized" {
        Write-Host "⚠️ 设备未授权，正在等待..." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "请在手机上："
        Write-Host "1. 查看USB调试授权弹窗"
        Write-Host "2. 勾选'始终允许来自这台计算机'"
        Write-Host "3. 点击'允许'"
        Write-Host ""
        Write-Host "如果没有弹窗，试试："
        Write-Host "• 断开重连USB"
        Write-Host "• 设置→开发者选项→撤销USB调试授权，然后重新连接"
        Write-Host ""
        Write-Host "授权完成后重新运行此脚本"
    }
    
    "disconnected" {
        Write-Host "❌ 设备未连接" -ForegroundColor Red
        Write-Host ""
        Write-Host "请检查："
        Write-Host "• USB线连接"
        Write-Host "• 手机开发者选项已启用"
        Write-Host "• USB调试已开启"
    }
}

Write-Host ""
Write-Host "💡 其他可用命令：" -ForegroundColor Blue
Write-Host "adb devices          # 查看设备列表"
Write-Host "adb logcat           # 查看实时日志"
Write-Host "adb shell            # 进入设备shell"
