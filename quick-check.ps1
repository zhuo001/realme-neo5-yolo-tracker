# Android 开发环境快速检查

Write-Host "🔍 检查Android开发环境..." -ForegroundColor Cyan

# 检查常见的Android SDK位置
$possibleSdkPaths = @(
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk",
    "C:\Android\Sdk", 
    "D:\Android\Sdk"
)

$sdkFound = $false
foreach ($path in $possibleSdkPaths) {
    if (Test-Path $path) {
        Write-Host "✅ 找到Android SDK: $path" -ForegroundColor Green
        
        # 设置环境变量
        $env:ANDROID_HOME = $path
        $env:ANDROID_SDK_ROOT = $path
        $adbPath = Join-Path $path "platform-tools\adb.exe"
        
        if (Test-Path $adbPath) {
            Write-Host "✅ 找到ADB工具" -ForegroundColor Green
            
            # 添加到PATH
            $env:PATH += ";$(Join-Path $path 'platform-tools')"
            
            # 检查设备连接
            Write-Host "🔸 检查设备连接..." -ForegroundColor Cyan
            try {
                $devices = & $adbPath devices 2>&1
                Write-Host $devices
                
                if ($devices -match "device$") {
                    Write-Host "✅ 设备已连接" -ForegroundColor Green
                    $sdkFound = $true
                    break
                } else {
                    Write-Host "⚠️ 设备未连接" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "❌ ADB执行失败: $_" -ForegroundColor Red
            }
        }
    }
}

if (-not $sdkFound) {
    Write-Host "❌ 未找到Android SDK或设备未连接" -ForegroundColor Red
    Write-Host "请安装Android Studio: https://developer.android.com/studio" -ForegroundColor Blue
}
