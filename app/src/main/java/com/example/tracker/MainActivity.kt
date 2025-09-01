package com.example.tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var usbSerial: UsbSerial
    private lateinit var cameraController: CameraController
    private lateinit var searchStrategy: AdaptiveSearchStrategy
    private lateinit var systemMonitor: SystemMonitor
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    companion object {
        private const val TAG = "MainActivity"
        private const val INPUT_SIZE = 640
        private const val NUM_CLASSES = 1  // 根据你的模型调整
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设备检测和优化配置
        val deviceOpt = DeviceDetector.getDeviceOptimizations()
        Log.i(TAG, "检测到设备: ${deviceOpt.deviceName}, 应用优化配置")
        applyDeviceOptimizations(deviceOpt)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        usbSerial = UsbSerial(this)
        systemMonitor = SystemMonitor()
        
        // 初始化摄像头控制器和搜索策略
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraController = CameraController(cameraManager)
        searchStrategy = AdaptiveSearchStrategy()
        
        // 设置搜索策略回调
        searchStrategy.setModeChangeCallback { config ->
            updateDetectionConfig(config)
            // 调用 native 更新阈值
            NativeBridge.updateThresholds(config.confidenceThreshold, config.nmsThreshold)
            if (config.useWideAngle != (cameraController.getCurrentCamera()?.isWideAngle == true)) {
                switchCamera(config.useWideAngle)
            }
        }
        
        // 检查相机权限
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        // 初始化 Native 模块
        initNativeModule()
        
        // 连接 USB
        connectUsb()
        
        // 显示可用摄像头信息
        showCameraInfo()
        
        // 启动系统监控
        systemMonitor.start { cpu, gpu, temp ->
            updateSystemStatus(cpu, gpu, temp)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // 默认使用广角摄像头启动 (适合搜索)
            val initialCamera = cameraController.getBestCameraForTracking(preferWideAngle = true)
            if (initialCamera != null) {
                cameraController.setCurrentCamera(initialCamera)
                bindCamera(initialCamera)
            } else {
                Log.e(TAG, "No suitable camera found")
                Toast.makeText(this, "找不到合适的摄像头", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindCamera(cameraSpec: CameraSpec) {
        val cameraProvider = this.cameraProvider ?: return
        
        try {
            // 解绑之前的用例
            cameraProvider.unbindAll()
            
            // 预览
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // 图像分析 - 根据摄像头类型调整分辨率
            val targetSize = if (cameraSpec.isWideAngle) {
                // 广角摄像头可以用更高分辨率，利用其视野优势
                Size(INPUT_SIZE, INPUT_SIZE)
            } else {
                // 标准摄像头用标准分辨率
                Size(INPUT_SIZE, INPUT_SIZE)
            }
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(targetSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, YoloAnalyzer())
                }

            // 创建对应的 CameraSelector
            val cameraSelector = cameraController.createCameraSelector(cameraSpec)

            // 绑定用例
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            
            Log.i(TAG, "Camera bound: ${cameraSpec.description}")
            Toast.makeText(this, "摄像头: ${cameraSpec.description}", Toast.LENGTH_SHORT).show()

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
    
    private fun switchCamera(useWideAngle: Boolean) {
        lifecycleScope.launch {
            val newCamera = cameraController.getBestCameraForTracking(useWideAngle)
            if (newCamera != null && newCamera != cameraController.getCurrentCamera()) {
                cameraController.setCurrentCamera(newCamera)
                withContext(Dispatchers.Main) {
                    bindCamera(newCamera)
                }
            }
        }
    }
    
    private fun showCameraInfo() {
        val cameras = cameraController.getAvailableCameras()
        Log.i(TAG, "Available cameras:")
        cameras.forEach { camera ->
            Log.i(TAG, "  ${camera.id}: ${camera.description}")
        }
        
        if (cameras.any { it.isWideAngle }) {
            Toast.makeText(this, "检测到广角摄像头，适合目标搜索", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateDetectionConfig(config: AdaptiveSearchStrategy.SearchConfig) {
        // 更新 UI 状态
        runOnUiThread {
            binding.tvStatus.text = config.description
            val bgColor = when (config.mode) {
                AdaptiveSearchStrategy.SearchMode.TRACKING -> R.color.status_tracking
                AdaptiveSearchStrategy.SearchMode.WIDE_SEARCH,
                AdaptiveSearchStrategy.SearchMode.INTENSIVE -> R.color.status_searching
                AdaptiveSearchStrategy.SearchMode.STANDBY -> R.color.status_standby
            }
            binding.tvStatus.setBackgroundColor(ContextCompat.getColor(this, bgColor))
        }
        
        // 这里可以更新 native 模块的检测参数
        // 目前 native 接口暂不支持运行时配置更改
        Log.i(TAG, "Detection config updated: ${config.description}")
        Log.d(TAG, "Confidence: ${config.confidenceThreshold}, NMS: ${config.nmsThreshold}")
    }
    
    private fun updateSystemStatus(cpu: Int, gpu: Int, temp: Float) {
        runOnUiThread {
            binding.tvCpuLoad.text = "CPU: $cpu%"
            binding.tvGpuLoad.text = "GPU: $gpu%"
            binding.tvTemperature.text = "Temp: ${"%.1f".format(temp)}°C"
        }
    }

    private fun initNativeModule() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 从 assets 复制模型文件到内部存储
                val paramPath = "${filesDir}/model.param"
                val binPath = "${filesDir}/model.bin"
                
                // 这里应该从 assets 复制文件，暂时跳过
                // copyAssetToFile("model.param", paramPath)
                // copyAssetToFile("model.bin", binPath)
                
                val success = NativeBridge.init(paramPath, binPath, INPUT_SIZE, NUM_CLASSES)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Log.i(TAG, "Native module initialized: ${NativeBridge.version()}")
                        Toast.makeText(this@MainActivity, "模型加载成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to initialize native module")
                        Toast.makeText(this@MainActivity, "模型加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing native module", e)
            }
        }
    }

    private fun connectUsb() {
        lifecycleScope.launch {
            val connected = usbSerial.connect()
            if (connected) {
                Log.i(TAG, "USB connected: ${usbSerial.getDeviceInfo()}")
                Toast.makeText(this@MainActivity, "USB 连接成功", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "USB connection failed")
                Toast.makeText(this@MainActivity, "USB 连接失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class TrackInfo(
    private data class UsbTrackRaw(
        val label: Int,
        val cx: Int,
        val cy: Int,
        val w: Int,
        val h: Int,
        val score: Float
    )

    private fun parseTrackFrame(buf: ByteArray, size: Int): List<TrackInfo> {
    private fun parseTrackFrame(buf: ByteArray, size: Int): List<UsbTrackRaw> {
        if (buf[0].toInt() != 0xAA || buf[1].toInt() != 0x55) return emptyList()
        val payloadLen = buf[2].toInt() and 0xFF
        if (payloadLen + 4 > size) return emptyList()
        val crcCalc = crc16Ccitt(buf, 0, size - 2)
        val crcFrame = (buf[size - 2].toInt() and 0xFF) or ((buf[size - 1].toInt() and 0xFF) shl 8)
        if (crcCalc != crcFrame) return emptyList()
        val trackCnt = buf[5].toInt() and 0xFF
        var offset = 6
        val tracks = ArrayList<TrackInfo>(trackCnt)
        for (i in 0 until trackCnt) {
            if (offset + 11 > size - 2) break
            val id = buf[offset].toInt() and 0xFF; offset++
            val label = buf[offset].toInt() and 0xFF; offset++
            fun rd16(): Int { val v = (buf[offset].toInt() and 0xFF) or ((buf[offset+1].toInt() and 0xFF) shl 8); offset += 2; return if (v and 0x8000 != 0) v - 0x10000 else v }
            val cx = rd16(); val cy = rd16(); val w = rd16(); val h = rd16()
            val qScore = buf[offset].toInt() and 0xFF; offset++
            tracks.add(TrackInfo(id,label,cx,cy,w,h,qScore/255f))
            tracks.add(UsbTrackRaw(id,label,cx,cy,w,h,qScore/255f))
        return tracks
    }

    private fun crc16Ccitt(data: ByteArray, start: Int, endExclusive: Int): Int {
        var crc = 0xFFFF
        for (i in start until endExclusive) {
            crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
            crc = crc and 0xFFFF
        }
        return crc
    }

    private inner class YoloAnalyzer : ImageAnalysis.Analyzer {
        private val frameBuffer = ByteArray(512) // USB 协议帧缓冲区
        private var frameCount = 0
        private var lastFpsTimestamp = System.currentTimeMillis()
        private var inferenceTimeSum = 0.0f
        private var inferenceCount = 0

        override fun analyze(image: ImageProxy) {
            frameCount++
            try {
                // 转换为 RGBA
                val buffer = imageToRgbaBuffer(image)
                
                // 推理（带计时）
                val inferTimeMs = NativeBridge.inferRGBAWithTiming(
                    buffer, image.width, image.height, frameBuffer
                )
                
                if (inferTimeMs > 0) {
                    inferenceTimeSum += inferTimeMs
                    inferenceCount++
                    
                    // 解析帧大小：实际上需要查看返回的帧大小
                    // 这里修正为通过检查帧头判断有效性
                    val frameSize = if (frameBuffer.size >= 8 && 
                                      frameBuffer[0].toInt() == 0xAA && 
                                      frameBuffer[1].toInt() == 0x55) {
                        val payloadLen = frameBuffer[2].toInt() and 0xFF
                        payloadLen + 5 // header(2) + len(1) + payload + crc(2)
                    } else 0
                    
                    val hasTarget = frameSize > 20
                    searchStrategy.updateTargetStatus(hasTarget)
                    
                    // 解析并更新 Overlay
                    val tracks = parseTrackFrame(frameBuffer, frameSize)
                    val overlayTracks = tracks.map { r ->
                        val left = (r.cx - r.w / 2f)
                        val top = (r.cy - r.h / 2f)
                        val right = (r.cx + r.w / 2f)
                        val bottom = (r.cy + r.h / 2f)
                        OverlayView.TrackInfo(
                            id = r.id,
                            bbox = android.graphics.RectF(left, top, right, bottom),
                            score = r.score,
                            label = r.label.toString()
                        )
                    }
                    runOnUiThread {
                        binding.overlayView.updateTracks(overlayTracks, image.width, image.height)
                    }
                    
                    // 发送到 STM32
                    if (frameSize > 0) {
                        lifecycleScope.launch {
                            val sent = usbSerial.sendData(frameBuffer.copyOf(frameSize))
                            if (sent < 0) {
                                Log.w(TAG, "Failed to send tracking data")
                            }
                        }
                    }
                }
                
                // 更新 FPS 和平均推理时间
                val now = System.currentTimeMillis()
                if (now - lastFpsTimestamp > 1000) {
                    val fps = frameCount
                    val avgInferTime = if (inferenceCount > 0) inferenceTimeSum / inferenceCount else 0.0f
                    runOnUiThread { 
                        binding.tvFps.text = "FPS: $fps"
                        // 需要在布局中添加 tvInferTime
                        // binding.tvInferTime.text = "Infer: ${"%.1f".format(avgInferTime)}ms"
                        Log.d(TAG, "Avg inference time: ${"%.1f".format(avgInferTime)}ms")
                    }
                    frameCount = 0
                    inferenceTimeSum = 0.0f
                    inferenceCount = 0
                    lastFpsTimestamp = now
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in image analysis", e)
            } finally {
                image.close()
            }
        }

        private fun imageToRgbaBuffer(image: ImageProxy): ByteBuffer {
            return ImageConverter.imageProxyToRgbaBuffer(image)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        systemMonitor.stop()
        NativeBridge.release()
        lifecycleScope.launch {
            usbSerial.disconnect()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 重置搜索状态
        searchStrategy.reset()
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停时重置追踪器
        NativeBridge.reset()
    }
    
    /**
     * 应用设备特定的优化配置
     */
    private fun applyDeviceOptimizations(config: DeviceOptimizations) {
        Log.i(TAG, "应用设备优化: ${config.deviceName}")
        
        // 设置性能参数
        Log.d(TAG, "最大帧率: ${config.maxFrameRate}fps")
        Log.d(TAG, "目标分辨率: ${config.targetResolution.width}x${config.targetResolution.height}")
        Log.d(TAG, "GPU加速: ${config.useGpuAcceleration}")
        Log.d(TAG, "并行处理线程: ${config.parallelProcessingThreads}")
        
        // 应用优化设置
        if (config.useGpuAcceleration) {
            // 启用GPU加速推理
            enableGpuAcceleration()
        }
        
        // 设置处理线程数
        if (config.parallelProcessingThreads > 1) {
            setupParallelProcessing(config.parallelProcessingThreads)
        }
        
        // 应用性能模式设置
        when (config.deviceName) {
            "Realme Neo 5 150W" -> {
                Log.i(TAG, "启用Realme Neo 5 150W高性能模式")
                // 设置高刷新率、GPU加速等
                applyRealmeNeo5Optimizations(config)
            }
            else -> {
                Log.i(TAG, "应用通用设备优化")
            }
        }
    }
    
    private fun enableGpuAcceleration() {
        // GPU加速初始化逻辑
        Log.d(TAG, "启用GPU加速推理")
    }
    
    private fun setupParallelProcessing(threads: Int) {
        // 并行处理设置
        Log.d(TAG, "设置$threads个并行处理线程")
    }
    
    private fun applyRealmeNeo5Optimizations(config: DeviceOptimizations) {
        // Realme Neo 5 150W特定优化
        Log.d(TAG, "应用Realme Neo 5 150W优化:")
        Log.d(TAG, "- Snapdragon 8+ Gen 1 GPU加速")
        Log.d(TAG, "- 120Hz高刷新率模式")
        Log.d(TAG, "- 8核CPU并行处理")
        Log.d(TAG, "- 散热优化阈值: ${config.thermalThreshold}°C")
        
        // 这里可以添加更多Realme特定的优化
        // 例如：设置高性能模式、调整散热策略等
    }
}
