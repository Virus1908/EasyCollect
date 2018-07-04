package vus.danylo.easycollect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BoundsView(ctx: Context, attrs: AttributeSet) : View(ctx, attrs) {

    private val lineLength = resources.getDimension(R.dimen.line_length)
    private val lineWidth = resources.getDimension(R.dimen.line_width)
    private val paint = Paint()

    init {
        paint.style = Paint.Style.FILL
        paint.color = resources.getColor(R.color.colorPrimary)
        paint.strokeWidth = lineWidth
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            val width = canvas.width
            val height = canvas.height
            val startX = lineWidth / 2
            val endX = width - lineWidth / 2
            val difference = height - width
            val topY: Float = (difference / 2).toFloat()
            val bottomY: Float = topY + width
            val line = floatArrayOf(
                    startX, topY + lineLength, startX, topY,
                    0f, topY, lineLength, topY,
                    startX, bottomY - lineLength, startX, bottomY,
                    0f, bottomY, lineLength, bottomY,
                    endX, topY + lineLength, endX, topY,
                    width.toFloat(), topY, endX - lineLength, topY,
                    endX, bottomY - lineLength, endX, bottomY,
                    width.toFloat(), bottomY, endX - lineLength, bottomY
            )
            canvas.drawLines(line, paint)
        }
    }
}