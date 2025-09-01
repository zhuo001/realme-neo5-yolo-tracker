# VS Code Android 开发完整指南

## 快速开始

### 1. 环境检查
```powershell
# 验证工具链
java -version          # 应显示 JDK 11+
adb version           # Android Debug Bridge
gradlew.bat --version # Gradle Wrapper
```

### 2. 设备连接
```powershell
# 连接 K30S Ultra
adb devices           # 应显示设备序列号
adb shell getprop ro.build.version.release  # Android 版本
```

### 3. 构建流程
```powershell
# 清理构建缓存
gradlew.bat clean

# 构建 Debug 版本
gradlew.bat assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.example.tracker/.MainActivity
```

## VS Code 任务使用

### 构建任务
| 任务名称 | 快捷键 | 说明 |
|----------|--------|------|
| `gradle: clean` | Ctrl+Shift+P → Tasks: Run Task | 清理构建产物 |
| `gradle: assembleDebug` | 无 | 构建 Debug APK |
| `gradle: assembleRelease` | 无 | 构建 Release APK |
| `native: build only` | 无 | 仅构建 C++ 原生库 |

### 部署任务
| 任务名称 | 依赖关系 | 说明 |
|----------|----------|------|
| `apk: installDebug` | 依赖 assembleDebug | 构建并安装 |
| `run: start Activity` | 依赖 installDebug | 安装并启动应用 |

### 调试任务
| 任务名称 | 输出 | 说明 |
|----------|------|------|
| `adb: logcat (filtered)` | 过滤应用日志 | 实时查看 YoloTracker 相关日志 |
| `adb: pidof tracker` | 进程 PID | 获取应用进程 ID |
| `perf: gfxinfo` | 性能数据 | 图形性能分析 |

### 测试任务
| 任务名称 | 覆盖范围 | 说明 |
|----------|----------|------|
| `tests: unit` | Kotlin 单元测试 | CRC16、帧解析验证 |
| `lint: debug` | 代码质量检查 | Android Lint 静态分析 |

## 调试配置

### Java/Kotlin 调试
```powershell
# 方法1：Android Studio 调试
# 1. 在 Android Studio 中打开项目
# 2. 设置断点
# 3. Debug app 配置运行

# 方法2：命令行 JDWP 调试
adb shell am set-debug-app -w com.example.tracker
adb shell am start -n com.example.tracker/.MainActivity
adb forward tcp:5005 jdwp:(pidof com.example.tracker)
# 然后在 IDE 中 attach 到 localhost:5005
```

### C++ Native 调试
```powershell
# 1. 准备 gdbserver
adb push %ANDROID_NDK_HOME%/prebuilt/android-arm64/gdbserver /data/local/tmp/gdbserver
adb shell chmod +x /data/local/tmp/gdbserver

# 2. 获取应用 PID
adb shell pidof com.example.tracker

# 3. 启动 gdbserver（替换 <PID>）
adb shell /data/local/tmp/gdbserver :5039 --attach <PID>

# 4. 在 VS Code 中启动 "Attach Native (gdb) arm64" 配置
# 输入上述 PID
```

### 性能分析
```powershell
# 图形性能
adb shell dumpsys gfxinfo com.example.tracker

# 内存使用
adb shell dumpsys meminfo com.example.tracker

# CPU 使用
adb shell top -p $(adb shell pidof com.example.tracker)

# 温度监控
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

## 日志分析

### 关键日志标签
```powershell
# 应用主日志
adb logcat | findstr "YoloTracker"

# Native 层日志
adb logcat | findstr "yolo_tracker"

# 相机日志
adb logcat | findstr "CameraController"

# USB 通信日志
adb logcat | findstr "UsbSerial"

# 搜索策略日志
adb logcat | findstr "SearchStrategy"
```

### 性能日志
```powershell
# 推理耗时
adb logcat | findstr "Avg inference time"

# FPS 监控
adb logcat | findstr "FPS:"

# 系统状态
adb logcat | findstr -E "(CPU|GPU|Temp):"
```

## 故障排查

### 构建问题
```powershell
# NDK 路径问题
echo $ANDROID_NDK_HOME
# 应指向有效 NDK 目录

# NCNN 库缺失
ls ncnn-android/lib/android/arm64-v8a/libncnn.a
# 确保预编译库存在

# CMake 版本不匹配
cmake --version
# 应为 3.22.1 或兼容版本
```

### 运行时问题
```powershell
# 权限检查
adb shell pm list permissions | findstr CAMERA

# 模型文件
adb shell ls /data/data/com.example.tracker/files/
# 应包含 model.param 和 model.bin

# USB 设备识别
adb shell lsusb
# 查看 STM32 是否被识别为 CDC 设备
```

### 性能问题
```powershell
# 热节流检查
adb shell cat /sys/class/thermal/thermal_zone0/temp
# 温度过高 (>80°C) 会影响性能

# 内存不足
adb shell cat /proc/meminfo | findstr Available
# 可用内存应 >500MB

# CPU 频率
adb shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq
# 检查 CPU 是否被限频
```

## 优化技巧

### 构建优化
```powershell
# 并行构建
gradlew.bat assembleDebug --parallel --max-workers=4

# 增量构建
gradlew.bat assembleDebug --configure-on-demand

# 跳过不必要的任务
gradlew.bat assembleDebug -x lint -x test
```

### 调试优化
```powershell
# 减少日志输出
adb logcat -c  # 清空日志缓冲区
adb logcat -s YoloTracker:I  # 仅显示 INFO 级别以上

# 目标化过滤
adb logcat | findstr -E "(ERROR|WARN|inference)"
```

### 部署优化
```powershell
# 快速部署（仅更新代码）
adb install -r -d app/build/outputs/apk/debug/app-debug.apk

# 直接替换 SO 库（高级）
adb push app/build/intermediates/cmake/debug/obj/arm64-v8a/libyolo_tracker.so /data/local/tmp/
adb shell run-as com.example.tracker cp /data/local/tmp/libyolo_tracker.so lib/arm64/
```

## 性能基准
在 K30S Ultra (骁龙865) 上的预期性能：

| 指标 | 目标值 | 备注 |
|------|--------|------|
| FPS | 15-30 | 640×640 输入时 |
| 推理耗时 | 30-60ms | NCNN FP16 |
| 内存使用 | <200MB | 包含模型与缓冲区 |
| CPU 占用 | <40% | 正常追踪模式 |
| 温度上升 | <10°C | 连续运行30分钟 |

## 扩展开发

### 添加新功能
1. 修改 native 接口（如需要）
2. 更新 JNI 声明
3. 实现 Kotlin 逻辑
4. 添加对应单元测试
5. 更新 tasks.json（如需要新任务）

### 模型升级
1. 转换新模型为 NCNN 格式
2. 检查输入/输出 blob 名称
3. 调整 `detector.cpp` 解码逻辑（如结构变化）
4. 更新 `YoloConfig` 参数
5. 重新测试性能基准

这套配置支持从原型开发到生产部署的完整工作流。根据具体需求调整构建、调试和优化策略。
