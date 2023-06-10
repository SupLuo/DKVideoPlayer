package unics.player.control.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class FloatIndicatorSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)


    }

    override fun getThumbOffset(): Int {
        return super.getThumbOffset()
    }
}