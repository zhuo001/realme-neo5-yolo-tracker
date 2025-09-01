# K30S Ultra 部署指南

## 1. 环境准备

### 1.1 开发环境
- Android Studio Hedgehog | 2023.1.1 或更新版本
- NDK 25.1.8937393 或更新版本
- CMake 3.22.1
- JDK 11

### 1.2 下载 NCNN Android 预编译库
```bash
# 下载最新的 ncnn-android 包
wget https://github.com/Tencent/ncnn/releases/download/20240102/ncnn-20240102-android.zip
unzip ncnn-20240102-android.zip
# 解压到项目根目录，确保路径为 ./ncnn-android/
```

### 1.3 准备模型文件
将训练好的 YOLOv8/v10 模型转换为 NCNN 格式：
```bash
# 导出 ONNX
yolo export model=best.pt format=onnx opset=13 dynamic=False

# 简化 ONNX (可选)
python -m onnxsim best.onnx best_sim.onnx

# 转换为 NCNN
./onnx2ncnn best_sim.onnx model.param model.bin

# 优化 (可选)
./ncnnoptimize model.param model.bin model_opt.param model_opt.bin 65536
```

## 2. K30S Ultra 广角摄像头配置

### 2.1 摄像头规格
K30S Ultra 后置摄像头配置：
- **主摄**: 64MP，视角约 78°  
- **广角**: 13MP，视角约 120°  
- **微距**: 5MP，视角约 89°

### 2.2 广角摄像头优势
✅ **搜索范围扩大**: 120° 视野比标准 78° 提升 54% 覆盖面积  
✅ **快速重新捕获**: 目标移出主摄视野时，广角仍可能覆盖  
✅ **环境感知**: 更好的周边目标感知，适合机器人导航  
✅ **多目标追踪**: 同时追踪更多目标

### 2.3 自适应摄像头切换策略
```
目标追踪中 → 主摄 (高精度)
  ↓ 目标丢失 30 帧
广角搜索 → 广角摄像头 (大视野)
  ↓ 目标丢失 90 帧  
密集搜索 → 广角 + 低阈值 (最大召回)
  ↓ 10秒无目标
待机模式 → 广角 + 节能
```

## 3. K30S Ultra 特定配置

### 3.1 性能调优
在 `YoloConfig` 中针对骁龙 865 调整：
```cpp
YoloConfig cfg;
cfg.inputSize = 512;        // 从 640 降到 512 提升性能
cfg.numClasses = 1;         // 你的实际类别数
cfg.confThreshold = 0.3f;   // 提高阈值减少误检
cfg.nmsThreshold = 0.45f;
cfg.useFP16 = true;         // 启用 FP16 加速
```

### 3.2 线程优化
在 `detector.cpp` 中：
```cpp
ncnn::Option opt;
opt.num_threads = 4;                    // 骁龙 865 的大核数量
opt.use_fp16_arithmetic = true;         // FP16 计算
opt.use_fp16_storage = true;           // FP16 存储
opt.use_packing_layout = true;         // 优化内存布局
opt.use_shader_pack8 = false;          // 移动端通常关闭
opt.use_image_storage = false;
```

### 3.3 热管理
添加温度监控（在 MainActivity 中）：
```kotlin
private fun monitorThermal() {
    val thermalService = getSystemService(Context.THERMAL_SERVICE) as ThermalManager
    thermalService.addThermalStatusListener { status ->
        when (status) {
            ThermalManager.THERMAL_STATUS_SEVERE,
            ThermalManager.THERMAL_STATUS_CRITICAL -> {
                // 降低推理频率或输入尺寸
                reducePerfomance()
            }
        }
    }
}
```

## 4. 编译步骤

### 4.1 项目结构检查
确保目录结构如下：
```
android yolo/
├── app/                    # Android 应用
├── native/                 # C++ 核心库  
├── ncnn-android/          # NCNN 预编译库
├── models/                # 放置 .param 和 .bin 文件
├── build.gradle
├── settings.gradle
└── README.md
```

### 3.2 导入 Android Studio
1. 打开 Android Studio
2. File -> Open -> 选择项目根目录
3. 等待 Gradle 同步完成
4. Tools -> SDK Manager -> SDK Tools -> 安装 NDK 和 CMake

### 3.3 配置签名 (可选)
在 `app/build.gradle` 中：
```gradle
android {
    signingConfigs {
        release {
            keyAlias 'tracker'
            keyPassword 'password'
            storeFile file('tracker.keystore')
            storePassword 'password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ...
        }
    }
}
```

### 3.4 编译
```bash
# 清理
./gradlew clean

# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本
./gradlew assembleRelease
```

## 4. K30S Ultra 安装与测试

### 4.1 开启开发者选项
1. 设置 -> 我的设备 -> 全部参数 -> 连续点击 "MIUI版本" 7次
2. 设置 -> 更多设置 -> 开发者选项
3. 启用 "USB调试"
4. 启用 "USB安装" (允许通过USB安装应用)

### 4.2 安装应用
```bash
# 连接手机后
adb install app/build/outputs/apk/debug/app-debug.apk

# 或者直接在 Android Studio 中点击 Run
```

### 4.3 权限设置
安装后在手机上：
1. 设置 -> 应用管理 -> YoloTracker -> 权限管理
2. 允许 "相机" 权限
3. 允许 "安装其他应用" (如果需要)

### 4.4 USB-C 连接测试
1. 使用 USB-C 转 USB-A 转接头连接 STM32
2. 或使用 USB-C Hub 扩展多个 USB 接口
3. 确保 STM32 枚举为 CDC 设备 (Virtual COM Port)

## 5. 性能调试

### 5.1 日志查看
```bash
# 查看应用日志
adb logcat | grep YoloTracker

# 查看 native 日志  
adb logcat | grep "yolo_tracker"

# 查看性能数据
adb shell dumpsys gfxinfo com.example.tracker
```

### 5.2 性能分析
在代码中添加计时：
```kotlin
val startTime = System.currentTimeMillis()
// 推理代码
val inferTime = System.currentTimeMillis() - startTime
Log.d(TAG, "Inference time: ${inferTime}ms")
```

### 5.3 内存优化
```kotlin
// 在 MainActivity 中
override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    when (level) {
        TRIM_MEMORY_RUNNING_CRITICAL -> {
            // 释放非关键资源
            NativeBridge.reset()
        }
    }
}
```

## 6. 常见问题

### 6.1 NCNN 找不到
确保 ncnn-android 目录在项目根目录，并且包含：
- `include/ncnn/` 头文件
- `lib/android/arm64-v8a/libncnn.a`
- `lib/android/armeabi-v7a/libncnn.a`

### 6.2 模型推理失败
检查：
- `.param` 文件中的 blob 名称是否与代码中的 "images" 和 "output" 匹配
- 模型输入尺寸是否为 640x640
- 类别数是否正确

### 6.3 USB 连接失败
- 检查 STM32 是否正确配置为 USB CDC 设备
- 查看 `device_filter.xml` 中的 vendor-id 和 product-id 是否匹配
- 使用 `adb shell lsusb` 查看设备是否被识别

### 6.4 性能问题
- 降低输入尺寸 (640 -> 512 -> 480)
- 启用 FP16 量化
- 减少推理频率 (跳帧)
- 关闭不必要的后台应用

## 7. 生产部署建议

1. **签名**: 使用正式签名证书签名应用
2. **混淆**: 启用 ProGuard 减小 APK 体积
3. **多渠道**: 为不同设备创建优化版本
4. **热更新**: 考虑模型文件的在线更新机制
5. **监控**: 集成 Crashlytics 等崩溃监控

## 8. 后续优化

1. **量化**: 转换为 INT8 模型提升性能
2. **蒸馏**: 使用知识蒸馏进一步压缩模型
3. **异构计算**: 利用 GPU/NPU 加速 (Qualcomm Hexagon DSP)
4. **多传感器**: 融合 IMU 数据提升追踪稳定性
