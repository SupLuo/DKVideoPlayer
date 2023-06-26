package unics.player.control.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar

class TimeShiftProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ProgressBar(context, attrs), TimeShiftBar