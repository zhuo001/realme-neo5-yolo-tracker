package com.example.tracker

import android.os.Build
import android.util.Log

object DeviceDetector {
    private const val TAG = "DeviceDetector"
    
    fun isRealmeNeo5(): Boolean {
        val model = Build.MODEL
        val device = Build.DEVICE
        Log.d(TAG, "Device Model: $model, Device: $device")
        
        return model.contains("RMX3706") || 
               model.contains("Realme Neo 5") ||
               device.contains("RE54ABL1")
    }
    
    fun getDeviceOptimizations(): DeviceOptimization {
        return when {
            isRealmeNeo5() -> {
                Log.i(TAG, "检测到Realme Neo 5 150W，启用高性能优化")
                DeviceOptimization(
                    cpuCores = 8,
                    preferredResolution = Pair(1920, 1080),
                    targetFps = 60,
                    useGpuAcceleration = true,
                    powerOptimization = false,
                    displayRefreshRate = 120
                )
            }
            Build.MODEL.contains("K30S") -> {
                Log.i(TAG, "检测到K30S Ultra，启用标准优化")
                DeviceOptimization(
                    cpuCores = 6,
                    preferredResolution = Pair(1280, 720),
                    targetFps = 30,
                    useGpuAcceleration = true,
                    powerOptimization = true,
                    displayRefreshRate = 60
                )
            }
            else -> {
                Log.i(TAG, "通用设备配置")
                DeviceOptimization(
                    cpuCores = 4,
                    preferredResolution = Pair(1280, 720),
                    targetFps = 30,
                    useGpuAcceleration = false,
                    powerOptimization = true,
                    displayRefreshRate = 60
                )
            }
        }
    }
}

data class DeviceOptimization(
    val cpuCores: Int,
    val preferredResolution: Pair<Int, Int>,
    val targetFps: Int,
    val useGpuAcceleration: Boolean,
    val powerOptimization: Boolean,
    val displayRefreshRate: Int
)
