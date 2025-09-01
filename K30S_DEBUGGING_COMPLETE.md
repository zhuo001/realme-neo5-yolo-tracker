# K30S Ultra 在设备调试完整方案

## 🎯 总结：你已获得完整的 K30S Ultra 调试能力

### ✅ 已完成配置
1. **VS Code 开发环境**：完整的 tasks.json + launch.json
2. **Native C++ 调试**：gdb 附加配置 + 符号文件路径
3. **自动化脚本**：`k30s-debug.ps1` 一键部署监控
4. **分层调试指南**：从基础到高级的排查流程
5. **性能监控集成**：实时 FPS + 推理耗时 + 系统状态

## 🚀 在 K30S Ultra 上调试的3种方式

### 方式1：VS Code 任务（推荐新手）
```
Ctrl+Shift+P → Tasks: Run Task → 选择：

构建部署：
- gradle: assembleDebug
- apk: installDebug  
- run: start Activity

K30S 专用：
- k30s: device info
- k30s: grant permissions
- k30s: performance monitor
- k30s: thermal status

调试准备：
- debug: prepare gdbserver
- debug: get app pid
```

### 方式2：PowerShell 脚本（推荐熟练用户）
```powershell
# 设备信息
.\k30s-debug.ps1 info

# 一键部署+监控
.\k30s-debug.ps1 deploy -Monitor

# 仅性能监控
.\k30s-debug.ps1 monitor

# Native调试准备
.\k30s-debug.ps1 debug
```

### 方式3：命令行手动（推荐专家）
```powershell
# 基础部署
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.example.tracker android.permission.CAMERA
adb shell am start -n com.example.tracker/.MainActivity

# 实时监控
adb logcat | Select-String -Pattern "(FPS:|inference|CPU:|GPU:|Temp:)"

# Native调试
adb push %ANDROID_NDK_HOME%\prebuilt\android-arm64\gdbserver /data/local/tmp/
$PID = adb shell pidof com.example.tracker
adb shell /data/local/tmp/gdbserver :5039 --attach $PID
# 然后 VS Code 启动 "Attach Native (gdb) arm64"
```

## 🔍 K30S Ultra 特定调试要点

### 硬件特性利用
- **骁龙865**：4个A77大核 + 4个A55小核，设置 `opt.num_threads = 4`
- **Adreno 650 GPU**：支持 Vulkan 1.1，可用于 YUV→RGBA 优化
- **LPDDR5**：12GB 内存，内存带宽充足
- **双摄像头**：主摄(78°) + 广角(120°)，自适应切换

### MIUI 调试注意事项
```
必须设置：
- 应用自启动：允许
- 后台活动：允许  
- 省电策略：无限制
- 锁屏显示：允许（防止息屏杀进程）

建议关闭：
- 智能省电
- 内存清理
- 后台应用限制
```

### 温度管理
```powershell
# 实时温度监控
adb shell 'for f in /sys/class/thermal/thermal_zone*/temp; do echo "$(basename $(dirname $f)): $(cat $f)°C"; done'

# 预期正常值：
# thermal_zone0: 45000°C  (45°C - CPU温度)
# thermal_zone1: 42000°C  (42°C - GPU温度)
# thermal_zone2: 40000°C  (40°C - 电池温度)

# >70°C 时应用会自动降频
```

## 🐛 常见问题快速解决

### 1. 应用启动失败
```powershell
# 检查崩溃原因
adb logcat | Select-String "AndroidRuntime"

# 常见原因：
# - 模型文件缺失 → 检查 assets/ 目录
# - 权限被拒绝 → 手动授予相机权限  
# - 内存不足 → 重启设备或关闭后台应用
```

### 2. 相机无预览
```powershell
# 检查相机占用
adb shell lsof | Select-String "/dev/video"

# 强制停止相机应用
adb shell am force-stop com.android.camera
adb shell am force-stop com.miui.camera

# 重启相机服务
adb shell stop media && adb shell start media
```

### 3. 性能过低
```powershell
# 检查 CPU 频率（是否被限频）
adb shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq

# 检查温度（是否热节流）
adb shell cat /sys/class/thermal/thermal_zone0/temp

# 优化建议：
# - 降低输入尺寸：640→512→480
# - 启用 FP16：cfg.useFP16 = true
# - 减少推理频率：跳帧处理
```

### 4. USB 通信失败
```powershell
# 检查 STM32 识别
adb shell lsusb | Select-String -Pattern "(STM|CDC|USB)"

# 检查设备权限
adb shell ls -la /dev/bus/usb/*/*

# 重新枚举 USB
# 拔插 STM32 设备，查看 dmesg
adb shell dmesg | Select-String "usb"
```

## 📊 性能基准验证

### 在 K30S Ultra 上的预期表现
```
✅ 正常指标：
- FPS: 25-30 (640×640 输入)
- 推理耗时: 30-50ms (NCNN FP16)
- CPU使用: 30-45%
- 温度: 45-60°C
- 内存: 100-150MB

⚠️ 需优化：
- FPS: <20
- 推理耗时: >80ms  
- CPU使用: >60%
- 温度: >70°C
- 内存: >200MB
```

### 实时监控命令
```powershell
# 一键性能监控
.\k30s-debug.ps1 monitor

# 或 VS Code 任务
Tasks: Run Task → "k30s: performance monitor"

# 预期日志输出：
# YoloTracker: FPS: 28
# YoloTracker: Avg inference time: 42.1ms  
# YoloTracker: CPU: 38% GPU: 25% Temp: 52.3°C
# SearchStrategy: Search mode: TRACKING -> WIDE_SEARCH
```

## 🎯 进阶调试技巧

### 1. 多目标追踪调试
```powershell
# 查看追踪状态
adb logcat | Select-String "ByteTrackLite"

# 协议帧内容
adb logcat | Select-String "Frame:" | Select-Object -Last 5
```

### 2. 自适应搜索调试  
```powershell
# 搜索模式切换
adb logcat | Select-String "Search mode:"

# 摄像头切换
adb logcat | Select-String "Camera bound:"
```

### 3. 深度性能分析
```powershell
# GPU 渲染分析
adb shell dumpsys gfxinfo com.example.tracker framestats

# 内存泄露检测
adb shell dumpsys meminfo com.example.tracker --package
```

## 🏆 最佳实践总结

1. **分层调试**：先基础功能，再复杂特性
2. **日志为王**：充分利用 logcat 过滤
3. **性能优先**：持续监控温度和资源使用
4. **自动化流程**：使用脚本减少重复操作
5. **文档记录**：记录问题解决方案供后续参考

**你现在拥有了在 K30S Ultra 上进行专业 Android 开发调试的完整工具链！** 🎉

从简单的应用部署到复杂的 Native C++ 调试，从性能监控到问题排查，所有工具都已就绪。根据你的技能水平选择合适的调试方式，开始在真机上验证你的 YOLO 追踪应用吧！
