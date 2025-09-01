# YUV → RGBA 转换优化指南

## 当前实现状态
项目中 `ImageConverter.imageProxyToRgbaBuffer()` 使用手工像素转换，适合原型开发但不是最优性能。

## 性能问题分析
1. **CPU 密集**：手工 YUV420→RGBA 转换在主线程执行
2. **内存拷贝**：多次 ByteBuffer 分配与复制
3. **无硬件加速**：未利用 GPU 或专用图像处理单元

## 优化方案

### 方案 1：RenderScript (推荐)
**优势**：成熟、硬件加速、Android 原生支持  
**劣势**：API Level 31+ 被弃用，需要兼容性考虑

```kotlin
// 添加依赖到 app/build.gradle
android {
    defaultConfig {
        renderscriptTargetApi 29
        renderscriptSupportModeEnabled true
    }
}

// 实现示例
class RenderScriptConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    
    fun convertYuvToRgba(image: ImageProxy): Allocation {
        val yuvBuffer = imageProxyToByteArray(image)
        val yuvAllocation = Allocation.createFromBitmap(rs, createYuvBitmap(yuvBuffer))
        val rgbaAllocation = Allocation.createTyped(rs, yuvAllocation.type)
        
        yuvToRgb.setInput(yuvAllocation)
        yuvToRgb.forEach(rgbaAllocation)
        
        return rgbaAllocation
    }
}
```

### 方案 2：Vulkan API (高级)
**优势**：最佳性能、现代 GPU 充分利用  
**劣势**：实现复杂度高、调试困难

```cpp
// native/vulkan_yuv_converter.h
class VulkanYuvConverter {
    VkDevice device;
    VkCommandPool commandPool;
    VkPipeline computePipeline;
    
public:
    bool initialize();
    bool convertYuv420ToRgba(uint8_t* yuvData, int width, int height, uint8_t* rgbaOut);
    void cleanup();
};
```

### 方案 3：OpenGL ES Shader (平衡)
**优势**：良好性能、跨平台兼容  
**劣势**：需要 GL context 管理

```kotlin
class GlYuvConverter {
    private val vertexShader = """
        attribute vec4 position;
        attribute vec2 texCoord;
        varying vec2 v_texCoord;
        void main() {
            gl_Position = position;
            v_texCoord = texCoord;
        }
    """
    
    private val fragmentShader = """
        precision mediump float;
        uniform sampler2D yTexture;
        uniform sampler2D uvTexture;
        varying vec2 v_texCoord;
        
        void main() {
            float y = texture2D(yTexture, v_texCoord).r;
            vec2 uv = texture2D(uvTexture, v_texCoord).rg - 0.5;
            
            float r = y + 1.403 * uv.y;
            float g = y - 0.344 * uv.x - 0.714 * uv.y;
            float b = y + 1.770 * uv.x;
            
            gl_FragColor = vec4(r, g, b, 1.0);
        }
    """
}
```

### 方案 4：Camera2 ImageReader (架构级)
**优势**：避免格式转换、直接 RGBA 输出  
**劣势**：需要重构相机管道

```kotlin
// 配置 ImageReader 直接输出 RGBA
val imageReader = ImageReader.newInstance(
    width, height, 
    ImageFormat.PRIVATE, // 让系统选择最优格式
    2
)

// 或者使用 SurfaceTexture
val surfaceTexture = SurfaceTexture(textureId)
val surface = Surface(surfaceTexture)
captureRequestBuilder.addTarget(surface)
```

## 性能测试基准
```kotlin
class ImageConversionBenchmark {
    @Test
    fun benchmarkConversionMethods() {
        val testImage = createTestYuvImage(640, 640)
        
        // 当前手工方法
        val manualTime = measureTimeMillis {
            repeat(100) { ImageConverter.imageProxyToRgbaBuffer(testImage) }
        }
        
        // RenderScript 方法
        val rsTime = measureTimeMillis {
            repeat(100) { renderScriptConverter.convert(testImage) }
        }
        
        println("Manual: ${manualTime}ms, RenderScript: ${rsTime}ms")
        assertTrue("RenderScript should be faster", rsTime < manualTime)
    }
}
```

## 实施建议
1. **短期**：保持当前手工实现，专注于功能完整性
2. **中期**：实施 RenderScript 方案（API < 31 设备）
3. **长期**：迁移到 Vulkan 或 ImageReader 架构

## K30S Ultra 特定考虑
- **骁龙 865**：支持 Vulkan 1.1、OpenGL ES 3.2
- **Adreno 650 GPU**：适合并行 YUV 转换
- **内存带宽**：LPDDR5 充足，但仍需避免不必要拷贝

## 预期性能提升
- **RenderScript**：2-4x 提升
- **Vulkan/OpenGL**：3-6x 提升  
- **ImageReader**：消除转换开销（最佳）

根据项目优先级选择实施方案。初期建议保持现状，完成核心功能后再优化转换性能。
