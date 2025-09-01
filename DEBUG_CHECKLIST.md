# K30S Ultra 调试检查清单

## 🔍 部署前检查

### ✅ 环境准备
- [ ] Android SDK 已安装且 `adb` 在 PATH 中
- [ ] JDK 11+ 已安装
- [ ] NDK 25.1.8937393+ 已安装 (如需 native 调试)
- [ ] `ANDROID_NDK_HOME` 环境变量已设置

### ✅ 设备配置
- [ ] K30S Ultra 已启用开发者选项
- [ ] USB调试已开启
- [ ] USB安装已允许
- [ ] 设备通过 `adb devices` 显示为 `device` 状态

### ✅ 项目文件
- [ ] `ncnn-android/` 预编译库已下载
- [ ] 模型文件 `model.param` 和 `model.bin` 已准备
- [ ] CMakeLists.txt 中 NCNN 路径正确

## 🚀 快速验证流程

### 1. 设备连接测试
```powershell
# 运行快速脚本
.\k30s-debug.ps1 info

# 预期输出：
# ✅ 设备已连接: Redmi K30S Ultra
# 设备型号: Redmi K30S Ultra
# Android版本: 11
# MIUI版本: MIUI12
# 芯片平台: kona (骁龙865)
```

### 2. 权限与设置验证
```powershell
# VS Code 任务
Tasks: Run Task → "k30s: grant permissions"

# 或命令行
adb shell pm grant com.example.tracker android.permission.CAMERA
adb shell pm list permissions -d -g | Select-String "CAMERA"
```

### 3. 应用部署测试
```powershell
# 自动化部署（推荐）
.\k30s-debug.ps1 deploy -Monitor

# 或 VS Code 任务
Tasks: Run Task → "run: start Activity"
```

## 🔧 分层调试策略

### Level 1: 基础功能验证
#### 目标：确保应用能正常启动
- [ ] APK 安装成功
- [ ] 应用启动无崩溃
- [ ] 相机权限获取成功
- [ ] 相机预览正常显示

**验证方法：**
```powershell
# 查看启动日志
adb logcat -c && adb logcat | Select-String "YoloTracker"

# 期望输出：
# YoloTracker: Native module initialized: tracker-native-0.1.0
# CameraController: Camera bound: 后置主摄
```

**常见问题：**
- 崩溃：检查模型文件路径
- 黑屏：检查相机权限
- 无响应：检查内存不足

### Level 2: 模型推理验证
#### 目标：确保 YOLO 检测正常工作
- [ ] 模型加载成功
- [ ] 推理返回有效结果
- [ ] FPS 在合理范围 (15-30)
- [ ] 推理耗时正常 (30-80ms)

**验证方法：**
```powershell
# 性能监控
.\k30s-debug.ps1 monitor

# 期望输出：
# YoloTracker: FPS: 25
# YoloTracker: Avg inference time: 45.2ms
```

**常见问题：**
- FPS 过低：检查输入尺寸、热节流
- 推理失败：检查模型 blob 名称
- 耗时过长：检查线程配置

### Level 3: 追踪与 USB 通信验证
#### 目标：确保多目标追踪与 STM32 通信正常
- [ ] 目标检测框显示正确
- [ ] 追踪 ID 稳定分配
- [ ] USB 设备识别成功
- [ ] 协议帧发送正常

**验证方法：**
```powershell
# USB 设备检查
Tasks: Run Task → "k30s: usb devices"

# 通信日志
adb logcat | Select-String "UsbSerial"

# 期望输出：
# UsbSerial: USB connected: STM32 CDC
# UsbSerial: Sent 23 bytes to STM32
```

**常见问题：**
- USB 不识别：检查设备权限
- 通信失败：检查协议帧格式
- 追踪不稳定：调整 IoU 阈值

### Level 4: 性能优化验证
#### 目标：确保系统性能最优
- [ ] CPU 使用率 <50%
- [ ] 温度 <70°C
- [ ] 内存使用稳定
- [ ] 自适应搜索正常工作

**验证方法：**
```powershell
# 系统状态监控
Tasks: Run Task → "k30s: thermal status"
Tasks: Run Task → "k30s: performance monitor"

# 搜索策略日志
adb logcat | Select-String "SearchStrategy"
```

## 🐛 常见问题快速排查

### 问题分类表

| 症状 | 可能原因 | 检查命令 | 解决方案 |
|------|----------|----------|----------|
| 应用崩溃 | 模型文件缺失 | `adb shell ls /data/data/com.example.tracker/files/` | 检查模型文件复制 |
| 相机黑屏 | 权限未授予 | `adb shell dumpsys package com.example.tracker \| grep CAMERA` | 手动授予权限 |
| FPS 过低 | 热节流 | `adb shell cat /sys/class/thermal/thermal_zone0/temp` | 降低输入尺寸 |
| USB 不通信 | 设备未识别 | `adb shell lsusb` | 检查 STM32 连接 |
| 内存不足 | 资源泄露 | `adb shell dumpsys meminfo com.example.tracker` | 重启应用 |

### 急救命令集

```powershell
# 强制停止应用
adb shell am force-stop com.example.tracker

# 清除应用数据
adb shell pm clear com.example.tracker

# 重启相机服务
adb shell stop media && adb shell start media

# 查看崩溃日志
adb logcat | Select-String -Pattern "(FATAL|AndroidRuntime|DEBUG)"

# 检查可用存储
adb shell df /data

# 检查 CPU 频率
adb shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq
```

## 🔍 Native 调试专项

### 准备工作
```powershell
# 1. 使用调试脚本
.\k30s-debug.ps1 debug

# 2. 或手动准备
Tasks: Run Task → "debug: prepare gdbserver"
Tasks: Run Task → "debug: get app pid"
```

### GDB 调试会话
```bash
# 在新的 PowerShell 窗口
adb shell /data/local/tmp/gdbserver :5039 --attach <PID>

# VS Code 中启动 "Attach Native (gdb) arm64"
# 常用 GDB 命令：
(gdb) info shared                    # 查看加载库
(gdb) b YoloDetector::infer          # 设断点
(gdb) bt                             # 查看调用栈
(gdb) p dets.size()                  # 打印变量
(gdb) x/16x frameBytes.data()        # 查看内存
```

### 调试技巧
- **符号加载**：确保 `libyolo_tracker.so` 包含调试符号
- **断点设置**：优先在关键函数设断点（`infer`, `buildTrackFrame`）
- **内存检查**：使用 `x` 命令检查协议帧内容
- **调用栈**：`bt` 命令查看崩溃位置

## 📊 性能基准对照

### K30S Ultra (骁龙865) 预期性能

| 指标 | 最佳值 | 良好值 | 需优化 |
|------|--------|--------|--------|
| FPS | >25 | 20-25 | <20 |
| 推理耗时 | <40ms | 40-60ms | >60ms |
| CPU 使用 | <30% | 30-50% | >50% |
| 温度 | <60°C | 60-70°C | >70°C |
| 内存 | <150MB | 150-200MB | >200MB |

### 优化建议
- **FPS 低**：降低 inputSize (640→512→480)
- **耗时长**：启用 FP16、增加线程数
- **CPU 高**：检查图像转换效率
- **过热**：限制推理频率、使用动态阈值

使用这个检查清单能系统化地排查 K30S Ultra 上的所有可能问题！
