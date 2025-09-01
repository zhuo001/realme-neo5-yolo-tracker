package com.example.tracker

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class SystemMonitor {
    
    private val handler = Handler(Looper.getMainLooper())
    private var monitorCallback: ((cpu: Int, gpu: Int, temp: Float) -> Unit)? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "SystemMonitor"
        private const val UPDATE_INTERVAL = 2000L // 2秒更新一次
    }
    
    fun start(callback: (cpu: Int, gpu: Int, temp: Float) -> Unit) {
        if (isRunning) return
        monitorCallback = callback
        isRunning = true
        handler.post(monitorRunnable)
        Log.i(TAG, "System monitor started")
    }
    
    fun stop() {
        if (!isRunning) return
        isRunning = false
        handler.removeCallbacks(monitorRunnable)
        Log.i(TAG, "System monitor stopped")
    }
    
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            
            val cpuLoad = getCpuLoad()
            val gpuLoad = getGpuLoad()
            val temperature = getTemperature()
            
            monitorCallback?.invoke(cpuLoad, gpuLoad, temperature)
            
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }
    
    private fun getCpuLoad(): Int {
        return try {
            val process = Runtime.getRuntime().exec("top -n 1")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var cpuUsage = 0
            
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("CPU")) {
                    // 简化解析：从 "idle" 中提取
                    val parts = line!!.split("\\s+".toRegex())
                    for (i in parts.indices) {
                        if (parts[i].contains("idle")) {
                            val idle = parts[i-1].replace("%", "").toInt()
                            cpuUsage = 100 - idle
                            break
                        }
                    }
                }
            }
            process.destroy()
            cpuUsage
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU load", e)
            -1
        }
    }
    
    private fun getGpuLoad(): Int {
        // 获取 GPU 负载需要 root 权限或特定 API
        // 这里返回一个模拟值
        return (30..70).random()
    }
    
    private fun getTemperature(): Float {
        return try {
            val process = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val temp = reader.readLine().toFloat() / 1000.0f
            process.destroy()
            temp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get temperature", e)
            -1.0f
        }
    }
}
