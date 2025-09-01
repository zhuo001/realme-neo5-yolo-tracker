package com.example.tracker

import android.util.Log
import kotlinx.coroutines.delay

/**
 * 自适应搜索策略 - 根据目标丢失情况调整搜索行为
 */
class AdaptiveSearchStrategy {
    
    companion object {
        private const val TAG = "SearchStrategy"
    }
    
    enum class SearchMode {
        TRACKING,      // 正常追踪模式
        WIDE_SEARCH,   // 广角搜索模式  
        INTENSIVE,     // 密集搜索模式
        STANDBY        // 待机模式
    }
    
    data class SearchConfig(
        val mode: SearchMode,
        val useWideAngle: Boolean,
        val confidenceThreshold: Float,
        val nmsThreshold: Float,
        val maxDetections: Int,
        val description: String
    )
    
    // 搜索配置预设
    private val searchConfigs = mapOf(
        SearchMode.TRACKING to SearchConfig(
            mode = SearchMode.TRACKING,
            useWideAngle = false,
            confidenceThreshold = 0.5f,
            nmsThreshold = 0.45f,
            maxDetections = 10,
            description = "标准追踪 - 高精度"
        ),
        SearchMode.WIDE_SEARCH to SearchConfig(
            mode = SearchMode.WIDE_SEARCH,
            useWideAngle = true,
            confidenceThreshold = 0.3f,
            nmsThreshold = 0.4f,
            maxDetections = 20,
            description = "广角搜索 - 扩大视野"
        ),
        SearchMode.INTENSIVE to SearchConfig(
            mode = SearchMode.INTENSIVE,
            useWideAngle = true,
            confidenceThreshold = 0.2f,
            nmsThreshold = 0.35f,
            maxDetections = 30,
            description = "密集搜索 - 低阈值"
        ),
        SearchMode.STANDBY to SearchConfig(
            mode = SearchMode.STANDBY,
            useWideAngle = true,
            confidenceThreshold = 0.4f,
            nmsThreshold = 0.45f,
            maxDetections = 15,
            description = "待机搜索 - 节能模式"
        )
    )
    
    private var currentMode = SearchMode.TRACKING
    private var targetLostFrames = 0
    private var lastTargetSeen = System.currentTimeMillis()
    private var modeChangeCallback: ((SearchConfig) -> Unit)? = null
    
    /**
     * 设置模式切换回调
     */
    fun setModeChangeCallback(callback: (SearchConfig) -> Unit) {
        modeChangeCallback = callback
    }
    
    /**
     * 更新目标状态
     * @param hasTarget 当前是否检测到目标
     * @param targetCount 检测到的目标数量
     */
    fun updateTargetStatus(hasTarget: Boolean, targetCount: Int = 0) {
        if (hasTarget) {
            // 目标找到，重置计数器
            if (targetLostFrames > 0) {
                Log.i(TAG, "Target reacquired after $targetLostFrames frames")
                targetLostFrames = 0
                lastTargetSeen = System.currentTimeMillis()
                switchToMode(SearchMode.TRACKING)
            }
        } else {
            // 目标丢失，增加计数器
            targetLostFrames++
        }
        
        // 根据丢失时长自适应切换模式
        adaptiveModeSwitching()
    }
    
    private fun adaptiveModeSwitching() {
        val timeSinceLast = System.currentTimeMillis() - lastTargetSeen
        val newMode = when {
            targetLostFrames == 0 -> SearchMode.TRACKING
            targetLostFrames in 1..30 -> currentMode // 保持当前模式，短暂丢失
            targetLostFrames in 31..90 -> SearchMode.WIDE_SEARCH
            targetLostFrames in 91..180 -> SearchMode.INTENSIVE
            timeSinceLast > 10000 -> SearchMode.STANDBY // 10秒无目标，进入待机
            else -> currentMode
        }
        
        if (newMode != currentMode) {
            switchToMode(newMode)
        }
    }
    
    private fun switchToMode(mode: SearchMode) {
        val oldMode = currentMode
        currentMode = mode
        val config = searchConfigs[mode]!!
        
        Log.i(TAG, "Search mode: ${oldMode.name} -> ${mode.name} (${config.description})")
        Log.d(TAG, "Config: conf=${config.confidenceThreshold}, wide=${config.useWideAngle}")
        
        modeChangeCallback?.invoke(config)
    }
    
    /**
     * 获取当前搜索配置
     */
    fun getCurrentConfig(): SearchConfig {
        return searchConfigs[currentMode]!!
    }
    
    /**
     * 手动设置搜索模式
     */
    fun setMode(mode: SearchMode) {
        switchToMode(mode)
    }
    
    /**
     * 获取当前模式
     */
    fun getCurrentMode(): SearchMode = currentMode
    
    /**
     * 获取目标丢失帧数
     */
    fun getTargetLostFrames(): Int = targetLostFrames
    
    /**
     * 重置搜索状态
     */
    fun reset() {
        targetLostFrames = 0
        lastTargetSeen = System.currentTimeMillis()
        switchToMode(SearchMode.TRACKING)
    }
    
    /**
     * 建议的摄像头切换策略
     */
    fun suggestCameraSwitch(): Boolean {
        return when (currentMode) {
            SearchMode.WIDE_SEARCH, SearchMode.INTENSIVE, SearchMode.STANDBY -> true
            SearchMode.TRACKING -> false
        }
    }
}
