# 🚀 Realme Neo 5 150W YOLO Camera Tracker

高性能移动端YOLO目标追踪器，专为Realme Neo 5 150W优化。

本模块聚焦：目标检测 + 多目标追踪（ByteTrack 精简版）+ 轨迹结果通过 USB-C 串行协议发送给 STM32；不在此仓库内实现底盘控制闭环。

## 🎯 项目特色

### 📱 设备特定优化 - Realme Neo 5 150W
- **目标设备**: Realme Neo 5 150W (RMX3706)
- **处理器**: Snapdragon 8+ Gen 1 专用优化
- **显示**: 120Hz高刷新率支持
- **GPU**: Adreno 730 GPU加速推理
- **内存**: 12GB LPDDR5 高效利用

### ⚡ 核心功能
- 🎮 **120FPS高刷新率**相机预览
- 🔥 **GPU加速**YOLO实时推理
- 💪 **8核并行**处理优化
- 🌡️ **85°C智能散热**管理
- 📱 **自动设备检测**和适配

## 目录结构
```
native/
  include/        # 头文件
  src/            # 实现
models/           # 放置 ncnn param/bin 或 tflite 模型
README.md
```

## 功能规划
- [x] 目录初始化
- [x] ByteTrack 精简实现 (C++)
- [x] USB 协议打包工具 (CRC16)
- [x] CMake 构建骨架
- [x] NCNN 推理包装类 (加载 + 预处理 + 解码 + NMS) - 需实际模型名称调整 blob
- [x] JNI 导出接口（init / infer / reset / release / getVersion）
- [x] 推理后处理（解码 + NMS）
- [x] Android 应用集成（CameraX + 自适应搜索 + USB 串行通信）
- [x] VS Code 开发环境（tasks.json + launch.json + native 调试）
- [x] 性能监控与优化（推理计时 + 动态阈值调整）
- [x] 单元测试框架（CRC16 + 帧解析验证）
- [x] YUV→RGBA 优化建议文档
## JNI 接口说明
Kotlin 使用示例：
```kotlin
object NativeBridge {
  init { System.loadLibrary("yolo_tracker") }
  external fun init(param:String, bin:String, inputSize:Int, numClasses:Int): Boolean
  external fun setImageSize(w:Int, h:Int)
  // buf: DirectByteBuffer (RGBA8888), outFrame: 预分配 ByteArray 存放协议帧
  external fun inferRGBA(buf:java.nio.ByteBuffer, w:Int, h:Int, outFrame:ByteArray): Int
  // 带性能计时的推理，返回推理耗时（毫秒）
  external fun inferRGBAWithTiming(buf:java.nio.ByteBuffer, w:Int, h:Int, outFrame:ByteArray): Float
  // 运行时更新检测阈值
  external fun updateThresholds(confThresh:Float, nmsThresh:Float)
  external fun reset()
  external fun release()
  external fun version():String
}
```
返回值：`inferRGBA` 若 >=0 表示写入的字节数；负数为错误码。`inferRGBAWithTiming` 返回正数为推理耗时毫秒，负数为错误。

错误码：
```
-1: 未初始化
-2: 缓冲区地址无效（需 DirectByteBuffer）
-3: outFrame 长度不足
```

注意：
- `ex.input("images", inF)` 与 `cfg.outName` 需对应实际 ncnn param 中的 blob 名称
- 若你的导出模型输入名/输出名不同，请改 `detector.cpp`
- 若模型结构非 (cx,cy,w,h,obj,cls...) 需修改 `decode`
- 使用 `updateThresholds()` 可根据搜索模式动态调整置信度与 NMS 阈值
- `inferRGBAWithTiming()` 提供性能监控，用于 FPS 与热管理优化

## VS Code 开发环境
项目已配置 `.vscode/tasks.json` 和 `.vscode/launch.json`：

**常用任务**：
- `gradle: assembleDebug` - 构建 Debug APK
- `apk: installDebug` - 安装到设备
- `run: start Activity` - 启动应用
- `adb: logcat (filtered)` - 查看应用日志
- `tests: unit` - 运行单元测试
- `adb: pidof tracker` - 获取进程 PID（供调试）

**调试配置**：
- `Attach Native (gdb) arm64` - C++ 代码调试（需要先配置 gdbserver）

使用 Ctrl+Shift+P → "Tasks: Run Task" 快速执行。

## 性能监控
应用实时显示：
- **FPS**: 相机帧处理频率
- **推理耗时**: YOLO 检测平均时间
- **CPU/GPU 负载**: 系统资源监控  
- **温度**: 设备热状态

自适应搜索策略根据目标丢失情况自动调整：
- **置信度阈值**: 0.2-0.5 动态范围
- **摄像头切换**: 标准 ↔ 广角
- **NMS 阈值**: 密集搜索时放宽

## 后续
添加 Android 工程时，将 `native` 作为 CMake 模块引入，JNI 层暴露：
```
init(modelParamPath, modelBinPath, inputSize)
infer(yuvImage, timestamp) -> Track[]
release()
```

USB 通讯协议帧（已在 `usb_protocol.*` 中实现 buildTrackFrame）：
```
Header: 0xAA 0x55
len   : 1 字节（后续 payload 长度，不含 header/len/CRC）
frame_id : uint16 (LE)
track_count : uint8
track block * N : id u8 | class u8 | cx i16 | cy i16 | w i16 | h i16 | score u8
CRC16 : CCITT(0x1021, init=0xFFFF) 低字节在前
```

Android 侧通过 UsbManager 找到 CDC/FTDI 设备，打开 BulkEndpoint 后直接写入 `buildTrackFrame` 返回的字节数组。

下一步建议优先：
1. **下载 NCNN**: 获取 `ncnn-android` 预编译库放到项目根目录
2. **准备模型**: 转换 YOLOv8/v10 为 `.param` 和 `.bin` 格式，放入 `app/src/main/assets/`
3. **编译测试**: 在 Android Studio 中编译并部署到 K30S Ultra
4. **USB 调试**: 连接 STM32，测试协议帧发送
5. **性能调优**: 根据实际帧率调整输入尺寸和线程配置

详细部署步骤请参考 `DEPLOYMENT.md`。
