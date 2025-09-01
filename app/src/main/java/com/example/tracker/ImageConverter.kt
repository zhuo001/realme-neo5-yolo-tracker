package com.example.tracker

import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object ImageConverter {
    
    /**
     * 将 CameraX ImageProxy 转换为 RGBA ByteBuffer
     */
    fun imageProxyToRgbaBuffer(image: ImageProxy): ByteBuffer {
        return when (image.format) {
            ImageFormat.YUV_420_888 -> yuv420ToRgba(image)
            ImageFormat.NV21 -> nv21ToRgba(image)
            else -> throw IllegalArgumentException("Unsupported image format: ${image.format}")
        }
    }
    
    private fun yuv420ToRgba(image: ImageProxy): ByteBuffer {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining() 
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        // 复制 Y 平面
        yBuffer.get(nv21, 0, ySize)
        
        // 交错复制 U 和 V 平面 (转换为 NV21 格式)
        val uvPixelStride = image.planes[1].pixelStride
        if (uvPixelStride == 1) {
            // U 和 V 连续存储
            uBuffer.get(nv21, ySize, uSize)
            vBuffer.get(nv21, ySize + uSize, vSize)
        } else {
            // U 和 V 交错存储，需要重新排列
            val uvBuffer = ByteArray(uSize)
            uBuffer.get(uvBuffer)
            
            for (i in 0 until uSize step uvPixelStride) {
                nv21[ySize + i / uvPixelStride] = uvBuffer[i]
            }
            
            val vvBuffer = ByteArray(vSize)
            vBuffer.get(vvBuffer)
            
            for (i in 0 until vSize step uvPixelStride) {
                nv21[ySize + uSize / uvPixelStride + i / uvPixelStride] = vvBuffer[i]
            }
        }
        
        return nv21ToRgbaBuffer(nv21, image.width, image.height)
    }
    
    private fun nv21ToRgba(image: ImageProxy): ByteBuffer {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return nv21ToRgbaBuffer(data, image.width, image.height)
    }
    
    private fun nv21ToRgbaBuffer(nv21: ByteArray, width: Int, height: Int): ByteBuffer {
        val rgbaBuffer = ByteBuffer.allocateDirect(width * height * 4)
        
        val frameSize = width * height
        var rgba: Int
        var y: Int
        var u: Int 
        var v: Int
        var r: Int
        var g: Int
        var b: Int
        
        for (i in 0 until height) {
            for (j in 0 until width) {
                y = nv21[i * width + j].toInt() and 0xFF
                
                val uvIndex = frameSize + (i shr 1) * width + (j and 1.inv())
                u = if (uvIndex < nv21.size) nv21[uvIndex].toInt() and 0xFF else 128
                v = if (uvIndex + 1 < nv21.size) nv21[uvIndex + 1].toInt() and 0xFF else 128
                
                // YUV 到 RGB 转换
                y = if (y < 16) 16 else y
                
                r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128))
                g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128))
                b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128))
                
                r = clamp(r, 0, 255)
                g = clamp(g, 0, 255)
                b = clamp(b, 0, 255)
                
                // RGBA 格式
                rgbaBuffer.put(r.toByte())
                rgbaBuffer.put(g.toByte()) 
                rgbaBuffer.put(b.toByte())
                rgbaBuffer.put(255.toByte()) // Alpha
            }
        }
        
        rgbaBuffer.rewind()
        return rgbaBuffer
    }
    
    private fun clamp(value: Int, min: Int, max: Int): Int {
        return Math.max(min, Math.min(max, value))
    }
}
