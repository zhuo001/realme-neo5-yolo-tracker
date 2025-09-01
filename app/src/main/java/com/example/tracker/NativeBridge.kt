package com.example.tracker

import java.nio.ByteBuffer

object NativeBridge {
    init {
        System.loadLibrary("yolo_tracker")
    }

    /**
     * 初始化 YOLO 检测器
     * @param paramPath NCNN param 文件路径
     * @param binPath NCNN bin 文件路径  
     * @param inputSize 输入尺寸 (通常 640)
     * @param numClasses 类别数
     * @return 是否成功
     */
    external fun init(paramPath: String, binPath: String, inputSize: Int, numClasses: Int): Boolean

    /**
     * 设置图像尺寸 (用于坐标映射)
     */
    external fun setImageSize(w: Int, h: Int)

    /**
     * RGBA 图像推理
     * @param buf DirectByteBuffer，RGBA8888 格式
     * @param w 图像宽度
     * @param h 图像高度  
     * @param outFrame 输出缓冲区，存放 USB 协议帧
     * @return 协议帧字节数，负数表示错误
     */
    external fun inferRGBA(buf: ByteBuffer, w: Int, h: Int, outFrame: ByteArray): Int

    /**
     * RGBA 图像推理（带计时）
     * @param buf DirectByteBuffer，RGBA8888 格式
     * @param w 图像宽度
     * @param h 图像高度  
     * @param outFrame 输出缓冲区，存放 USB 协议帧
     * @return 推理耗时（毫秒），负数表示错误
     */
    external fun inferRGBAWithTiming(buf: ByteBuffer, w: Int, h: Int, outFrame: ByteArray): Float

    /**
     * 运行时更新检测阈值
     * @param confThresh 置信度阈值 (0.0-1.0)
     * @param nmsThresh NMS 阈值 (0.0-1.0)
     */
    external fun updateThresholds(confThresh: Float, nmsThresh: Float)

    /**
     * 重置追踪器
     */
    external fun reset()

    /**
     * 释放资源
     */
    external fun release()

    /**
     * 获取版本信息
     */
    external fun version(): String
}
