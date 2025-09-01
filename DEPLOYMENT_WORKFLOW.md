# K30S Ultra 完整开发流程指南

## 当前状态
- ✅ ADB工具已配置 (platform-tools)
- ✅ 设备已检测到 (ID: 29967350)
- ⚠️ 等待USB调试授权

## 授权完成后的完整流程

### 第一步：验证设备连接
```powershell
# 应该显示 "device" 状态
adb devices

# 获取设备详细信息
adb shell getprop ro.product.model    # 应显示 "M2007J3SC" (K30S Ultra)
adb shell getprop ro.product.brand    # 应显示 "Redmi"
adb shell getprop ro.build.version.release  # Android版本
```

### 第二步：构建项目
```powershell
# 方法1: 使用Gradle (如果有Java环境)
.\gradlew assembleDebug

# 方法2: 使用Android Studio
# 打开项目 → Build → Build Bundle(s)/APK(s) → Build APK(s)

# 验证APK生成
Test-Path "app\build\outputs\apk\debug\app-debug.apk"
```

### 第三步：部署应用
```powershell
# 安装APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 授予权限
adb shell pm grant com.example.tracker android.permission.CAMERA
adb shell pm grant com.example.tracker android.permission.WRITE_EXTERNAL_STORAGE

# 启动应用
adb shell am start -n com.example.tracker/.MainActivity

# 验证应用启动
adb shell pidof com.example.tracker
```

### 第四步：实时监控
```powershell
# 清空日志并开始监控
adb logcat -c
adb logcat | Select-String -Pattern "(YoloTracker|FPS|inference|ERROR)"

# 监控系统状态 (新窗口运行)
adb shell "while true; do echo '=== CPU ==='; cat /proc/loadavg; echo '=== Memory ==='; cat /proc/meminfo | head -3; echo '=== Temp ==='; cat /sys/class/thermal/thermal_zone0/temp; sleep 5; done"
```

### 第五步：Native调试 (可选)
```powershell
# 获取应用PID
$pid = adb shell pidof com.example.tracker

# 推送gdbserver (需要NDK)
adb push $env:ANDROID_NDK_HOME\prebuilt\android-arm64\gdbserver /data/local/tmp/
adb shell chmod +x /data/local/tmp/gdbserver

# 启动gdbserver (新窗口)
adb shell "/data/local/tmp/gdbserver :5039 --attach $pid"

# 在VS Code中启动调试配置 "Attach Native (gdb) arm64"
```

## 预期性能指标

### K30S Ultra基准性能
- **FPS**: 25-30 (640×640输入)
- **推理延迟**: 30-50ms
- **CPU使用率**: 35-45%
- **内存占用**: ~400MB
- **温度**: <60°C (正常使用)

### 关键日志内容
```
YoloTracker: Inference time: 35ms
YoloTracker: FPS: 28.5
YoloTracker: Objects detected: 3
YoloTracker: Track IDs: [1, 2, 3]
```

## 常见问题排查

### 应用崩溃
```powershell
# 查看崩溃日志
adb logcat | Select-String -Pattern "(FATAL|AndroidRuntime)"

# 检查native崩溃
adb logcat | Select-String -Pattern "DEBUG"
```

### 性能问题
```powershell
# 检查CPU频率
adb shell "cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq"

# 检查GPU频率
adb shell "cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq"

# 检查温度
adb shell "cat /sys/class/thermal/thermal_zone*/temp"
```

### USB连接问题
```powershell
# 重启ADB服务
adb kill-server
adb start-server

# 检查USB设备
adb devices -l
```

## 优化建议

### 性能优化
1. **降低输入分辨率**: 640×640 → 416×416
2. **调整检测阈值**: confidence > 0.3
3. **限制最大检测数**: max_objects = 50

### 稳定性优化
1. **异常处理**: try-catch包围推理代码
2. **内存管理**: 及时释放Mat对象
3. **温度监控**: 超过65°C时降频

## 下一步开发

### 立即可做
- [x] ADB环境配置
- [ ] 设备USB授权 ← **当前步骤**
- [ ] 项目构建
- [ ] 应用部署

### 短期目标
- [ ] 基准性能测试
- [ ] 模型文件集成 (.param/.bin)
- [ ] USB协议调试

### 长期目标  
- [ ] 相机参数优化
- [ ] 多目标追踪调优
- [ ] 产品化封装

---

**当前任务**: 请在手机上授权USB调试，然后我们继续下一步的项目构建和部署。
