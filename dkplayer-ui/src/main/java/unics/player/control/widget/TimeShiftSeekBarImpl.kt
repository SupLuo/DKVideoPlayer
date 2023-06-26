package unics.player.control.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar

class TimeShiftSeekBarImpl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatSeekBar(context, attrs), TimeShiftSeekBar