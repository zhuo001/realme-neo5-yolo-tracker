package com.example.tracker

object RealmeNeo5Config {
    // Realme Neo 5 150W 设备优化配置
    const val DEVICE_MODEL = "RMX3706" // Realme Neo 5 150W
    const val OPTIMIZED_FOR_SNAPDRAGON_8_PLUS_GEN1 = true
    
    // 相机配置优化
    const val PREFERRED_CAMERA_RESOLUTION_WIDTH = 1920
    const val PREFERRED_CAMERA_RESOLUTION_HEIGHT = 1080
    const val TARGET_FPS = 60 // 利用高刷屏幕
    
    // YOLO 模型优化配置
    const val USE_GPU_ACCELERATION = true
    const val USE_HEXAGON_DSP = true // 骁龙DSP加速
    const val BATCH_SIZE = 1
    const val NUM_THREADS = 8 // 骁龙8+Gen1的8核心
    
    // 电源管理 (利用150W快充)
    const val ENABLE_POWER_OPTIMIZATION = false // 性能优先
    const val THERMAL_THROTTLING_THRESHOLD = 85.0f // 更高的温度阈值
    
    // 显示优化
    const val USE_HDR_DISPLAY = true
    const val REFRESH_RATE = 120 // 支持120Hz
}
