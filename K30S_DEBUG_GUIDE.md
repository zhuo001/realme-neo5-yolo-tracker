# K30S Ultra 调试完整指南

## 设备准备

### 1. 启用开发者模式
```
设置 → 我的设备 → 全部参数 → 连续点击"MIUI版本" 7次
```

### 2. 开发者选项配置
```
设置 → 更多设置 → 开发者选项：
✅ USB调试
✅ USB安装（允许通过USB安装应用）
✅ USB调试（安全设置）
✅ 停用adb授权超时功能
✅ 无线显示认证（可选）
```

### 3. 性能优化配置
```
开发者选项中：
✅ 强制进行GPU渲染
✅ 强制启用4x MSAA（可选，提升渲染质量）
❌ 不保留活动（避免应用被杀后台）
❌ 后台进程限制 → 标准限制
```

## USB 连接配置

### 1. 物理连接
```
K30S Ultra (USB-C) ←→ 电脑 (USB-A)
使用原装数据线或支持数据传输的USB-C线
```

### 2. 连接验证
```powershell
# 检查设备连接
adb devices

# 预期输出：
# List of devices attached
# <serial_number>    device

# 如果显示 unauthorized，在手机上允许USB调试授权
```

### 3. 网络调试（可选）
```powershell
# 有线连接后启用无线调试
adb tcpip 5555
adb connect <K30S_IP>:5555

# 验证无线连接
adb devices
# 应显示 <IP>:5555    device
```

## 应用调试流程

### 1. 快速部署测试
```powershell
# 方式1：VS Code 任务
# Ctrl+Shift+P → Tasks: Run Task → "run: start Activity"

# 方式2：命令行
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.tracker/.MainActivity

# 立即查看启动日志
adb logcat -c && adb logcat | Select-String "YoloTracker"
```

### 2. 权限调试
```powershell
# 检查相机权限
adb shell dumpsys package com.example.tracker | Select-String "permission"

# 手动授予权限（如果自动授权失败）
adb shell pm grant com.example.tracker android.permission.CAMERA

# 验证权限状态
adb shell pm list permissions -d -g | Select-String "CAMERA"
```

### 3. 模型文件调试
```powershell
# 检查模型文件是否正确复制
adb shell ls -la /data/data/com.example.tracker/files/

# 如果文件缺失，手动推送（调试用）
adb push model.param /sdcard/
adb push model.bin /sdcard/
adb shell run-as com.example.tracker cp /sdcard/model.param files/
adb shell run-as com.example.tracker cp /sdcard/model.bin files/
```

## 性能调试

### 1. 实时性能监控
```powershell
# CPU 使用率
adb shell top -p $(adb shell pidof com.example.tracker)

# 内存使用
adb shell dumpsys meminfo com.example.tracker

# GPU 性能
adb shell dumpsys gfxinfo com.example.tracker

# 温度监控
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

### 2. FPS 与推理耗时调试
```powershell
# 应用内日志
adb logcat | Select-String -Pattern "(FPS:|Avg inference time:|CPU:|GPU:|Temp:)"

# 预期输出示例：
# YoloTracker: FPS: 28
# YoloTracker: Avg inference time: 35.2ms
# YoloTracker: CPU: 45% GPU: 32% Temp: 42.1°C
```

### 3. 相机调试
```powershell
# 检查可用摄像头
adb logcat | Select-String "CameraController"

# 预期日志：
# CameraController: Available cameras:
# CameraController:   0: 后置主摄 (1080x1920, FOV: 78°)
# CameraController:   2: 后置广角 (1080x1920, FOV: 120°)

# 相机切换日志
adb logcat | Select-String "Camera bound:"
```

## USB 通信调试

### 1. STM32 设备识别
```powershell
# 查看USB设备列表
adb shell lsusb

# 检查CDC设备
adb shell ls /dev/tty*

# USB权限检查
adb shell ls -la /dev/bus/usb/*/*
```

### 2. USB 通信测试
```powershell
# 查看USB通信日志
adb logcat | Select-String "UsbSerial"

# 预期日志：
# UsbSerial: USB connected: STM32 CDC (VID:0483 PID:5740)
# UsbSerial: Sent 23 bytes to STM32
# UsbSerial: Frame: AA 55 0E 34 12 02 01 00 64 00 C8 00 32 00 50 00 CC 02 01...
```

### 3. 协议帧验证
```powershell
# 运行单元测试验证协议
adb shell am instrument -w -e class com.example.tracker.UsbProtocolTest com.example.tracker.test/androidx.test.runner.AndroidJUnitRunner

# 或通过 VS Code 任务
# Tasks: Run Task → "tests: unit"
```

## Native C++ 调试

### 1. 准备调试环境
```powershell
# 1. 推送 gdbserver 到设备
adb push %ANDROID_NDK_HOME%\prebuilt\android-arm64\gdbserver /data/local/tmp/gdbserver
adb shell chmod +x /data/local/tmp/gdbserver

# 2. 检查符号文件存在
ls app\build\intermediates\cmake\debug\obj\arm64-v8a\libyolo_tracker.so
```

### 2. 启动调试会话
```powershell
# 1. 启动应用（Debug版本）
adb shell am start -n com.example.tracker/.MainActivity

# 2. 获取进程PID
$PID = adb shell pidof com.example.tracker
echo "App PID: $PID"

# 3. 启动 gdbserver（在单独PowerShell窗口）
adb shell "/data/local/tmp/gdbserver :5039 --attach $PID"

# 4. 在VS Code中启动"Attach Native (gdb) arm64"配置
# 输入上述PID
```

### 3. 调试技巧
```powershell
# gdb 命令示例（在VS Code调试控制台）：
# (gdb) info shared                    # 查看加载的共享库
# (gdb) b YoloDetector::infer          # 在推理函数设断点
# (gdb) b buildTrackFrame              # 在协议构建处设断点
# (gdb) print dets.size()              # 打印检测结果数量
# (gdb) x/10x frameBytes.data()        # 查看协议帧内容
```

## 常见问题排查

### 1. 应用崩溃
```powershell
# 查看崩溃日志
adb logcat | Select-String -Pattern "(FATAL|AndroidRuntime|DEBUG)"

# 常见崩溃原因：
# - 模型文件路径错误
# - NCNN库缺失
# - JNI调用异常
# - 内存不足
```

### 2. 性能问题
```powershell
# 检查热节流
adb shell cat /sys/class/thermal/thermal_zone0/temp
# 温度 >75°C 会触发限频

# 检查CPU频率
adb shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq

# 内存压力
adb shell cat /proc/meminfo | Select-String "Available"
# 可用内存 <500MB 可能影响性能
```

### 3. 相机问题
```powershell
# 检查相机占用
adb shell lsof | Select-String "/dev/video"

# 强制停止可能冲突的相机应用
adb shell am force-stop com.android.camera
adb shell am force-stop com.miui.camera

# 重启相机服务
adb shell stop media
adb shell start media
```

### 4. USB 通信问题
```powershell
# 检查USB设备权限
adb shell ls -la /dev/bus/usb/*/*

# 重新插拔STM32设备后检查
adb shell dmesg | Select-String "usb"

# 检查应用USB权限
adb shell dumpsys package com.example.tracker | Select-String "usb"
```

## K30S Ultra 特定优化

### 1. 骁龙865性能设置
```powershell
# 设置性能模式（需要root）
# adb shell "echo performance > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor"
# adb shell "echo performance > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor"

# 非root设备：通过应用内检测自动调整
adb logcat | Select-String "Detection config updated"
```

### 2. MIUI 特定设置
```
设置 → 应用设置 → 应用管理 → YoloTracker：
✅ 自启动
✅ 后台活动
✅ 锁屏显示
❌ 省电策略 → 无限制
```

### 3. 广角摄像头验证
```powershell
# 验证广角摄像头切换
adb logcat | Select-String "Camera bound:"

# 预期日志：
# Camera bound: 后置广角 (1080x1920, FOV: 120°)
# 摄像头: 后置广角 (1080x1920, FOV: 120°)
```

## 调试日志分级

### 关键日志（必须正常）
```
✅ YoloTracker: Native module initialized: tracker-native-0.1.0
✅ YoloTracker: USB connected: STM32 CDC (VID:0483 PID:5740)
✅ CameraController: Camera bound: 后置主摄
```

### 性能日志（监控）
```
📊 YoloTracker: FPS: 25-30 (正常)
📊 YoloTracker: Avg inference time: 30-60ms (良好)
📊 YoloTracker: CPU: <50% (健康)
📊 Temp: <70°C (安全)
```

### 警告日志（需关注）
```
⚠️ SearchStrategy: Target reacquired after 45 frames (搜索策略切换)
⚠️ UsbSerial: Failed to send tracking data (USB通信异常)
⚠️ SystemMonitor: High CPU usage: 85% (性能告警)
```

### 错误日志（需解决）
```
❌ YoloTracker: Failed to initialize native module (模型加载失败)
❌ CameraController: No suitable camera found (相机初始化失败)
❌ UsbSerial: USB connection failed (USB设备问题)
```

这套调试流程能确保在 K30S Ultra 上快速定位和解决问题。记住：先验证基础功能（相机+模型），再调试高级功能（USB+性能）。
