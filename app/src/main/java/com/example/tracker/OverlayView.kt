package com.example.tracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    private val paint = Paint()
    private val textPaint = Paint()
    private val bboxColors: List<Int>
    
    private var tracks: List<TrackInfo> = emptyList()
    private var scaleX = 1.0f
    private var scaleY = 1.0f
    
    data class TrackInfo(
        val id: Int,
        val bbox: RectF,
        val score: Float,
        val label: String
    )
    
    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4.0f
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 32.0f
        textPaint.style = Paint.Style.FILL
        
        bboxColors = listOf(
            ContextCompat.getColor(context, R.color.bbox_color_1),
            ContextCompat.getColor(context, R.color.bbox_color_2),
            ContextCompat.getColor(context, R.color.bbox_color_3),
            ContextCompat.getColor(context, R.color.bbox_color_4),
            ContextCompat.getColor(context, R.color.bbox_color_5)
        )
    }
    
    fun updateTracks(newTracks: List<TrackInfo>, imageWidth: Int, imageHeight: Int) {
        this.tracks = newTracks
        this.scaleX = width.toFloat() / imageWidth
        this.scaleY = height.toFloat() / imageHeight
        postInvalidate() // 触发重绘
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (track in tracks) {
            // 选择颜色
            paint.color = bboxColors[track.id % bboxColors.size]
            
            // 缩放 BBox
            val scaledBbox = RectF(
                track.bbox.left * scaleX,
                track.bbox.top * scaleY,
                track.bbox.right * scaleX,
                track.bbox.bottom * scaleY
            )
            
            // 绘制圆角矩形
            canvas.drawRoundRect(scaledBbox, 16f, 16f, paint)
            
            // 绘制标签
            val label = "ID: ${track.id} (${(track.score * 100).toInt()}%)"
            val textBg = RectF(
                scaledBbox.left,
                scaledBbox.top - 40,
                scaledBbox.left + textPaint.measureText(label) + 16,
                scaledBbox.top
            )
            
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(textBg, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            
            canvas.drawText(label, scaledBbox.left + 8, scaledBbox.top - 8, textPaint)
        }
    }
}
