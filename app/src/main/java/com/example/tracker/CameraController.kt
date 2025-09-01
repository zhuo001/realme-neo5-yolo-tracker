package com.example.tracker

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlin.math.abs

data class CameraSpec(
    val id: String,
    val focalLength: Float,
    val fov: Float,
    val description: String,
    val isWideAngle: Boolean = false
)

class CameraController(private val cameraManager: CameraManager) {
    
    companion object {
        private const val TAG = "CameraController"
        private const val WIDE_ANGLE_FOV_THRESHOLD = 90f // 视场角大于90度认为是广角
    }
    
    private val availableCameras = mutableListOf<CameraSpec>()
    private var currentCameraSpec: CameraSpec? = null
    
    init {
        detectCameras()
    }
    
    private fun detectCameras() {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                // 只关注后置摄像头
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    
                    if (focalLengths != null && sensorSize != null && focalLengths.isNotEmpty()) {
                        val focalLength = focalLengths[0]
                        // 计算水平视场角 (FOV)
                        val fov = 2 * Math.toDegrees(kotlin.math.atan((sensorSize.width / (2 * focalLength)).toDouble())).toFloat()
                        
                        val isWide = fov > WIDE_ANGLE_FOV_THRESHOLD
                        val description = when {
                            fov > 110f -> "超广角 (${fov.toInt()}°)"
                            fov > 90f -> "广角 (${fov.toInt()}°)"
                            fov > 70f -> "标准 (${fov.toInt()}°)"
                            else -> "长焦 (${fov.toInt()}°)"
                        }
                        
                        val spec = CameraSpec(cameraId, focalLength, fov, description, isWide)
                        availableCameras.add(spec)
                        
                        Log.i(TAG, "Camera $cameraId: $description, focal=${focalLength}mm")
                    }
                }
            }
            
            // 按视场角排序，广角优先
            availableCameras.sortByDescending { it.fov }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting cameras", e)
        }
    }
    
    /**
     * 获取最佳摄像头用于目标搜索
     * @param preferWideAngle 是否优先使用广角
     */
    fun getBestCameraForTracking(preferWideAngle: Boolean = true): CameraSpec? {
        return if (preferWideAngle) {
            // 优先选择广角摄像头
            availableCameras.firstOrNull { it.isWideAngle } ?: availableCameras.firstOrNull()
        } else {
            // 选择标准摄像头
            availableCameras.firstOrNull { !it.isWideAngle } ?: availableCameras.firstOrNull()
        }
    }
    
    /**
     * 创建对应的 CameraSelector
     */
    fun createCameraSelector(cameraSpec: CameraSpec): CameraSelector {
        return CameraSelector.Builder()
            .addCameraFilter { cameraInfos ->
                cameraInfos.filter { cameraInfo ->
                    val cameraId = Camera2CameraInfo.from(cameraInfo).cameraId
                    cameraId == cameraSpec.id
                }
            }
            .build()
    }
    
    /**
     * 获取所有可用摄像头
     */
    fun getAvailableCameras(): List<CameraSpec> = availableCameras.toList()
    
    /**
     * 设置当前使用的摄像头
     */
    fun setCurrentCamera(spec: CameraSpec) {
        currentCameraSpec = spec
        Log.i(TAG, "Switched to camera: ${spec.description}")
    }
    
    /**
     * 获取当前摄像头规格
     */
    fun getCurrentCamera(): CameraSpec? = currentCameraSpec
    
    /**
     * 根据目标丢失情况自动切换摄像头
     */
    fun adaptiveCameraSwitch(targetLostFrames: Int): CameraSpec? {
        val current = currentCameraSpec ?: return null
        
        return when {
            // 目标丢失较久，切换到广角搜索
            targetLostFrames > 60 && !current.isWideAngle -> {
                Log.i(TAG, "Target lost, switching to wide-angle for search")
                getBestCameraForTracking(preferWideAngle = true)
            }
            // 目标重新找到，可以考虑切回标准镜头提高精度
            targetLostFrames == 0 && current.isWideAngle -> {
                Log.i(TAG, "Target found, considering standard camera for precision")
                getBestCameraForTracking(preferWideAngle = false) ?: current
            }
            else -> current
        }
    }
}
