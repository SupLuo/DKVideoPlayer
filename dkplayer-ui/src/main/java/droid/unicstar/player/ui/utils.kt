package droid.unicstar.player.ui

import android.view.View
import android.widget.TextView
import java.util.*

/**
 * 格式化时间
 */
fun Int.toTimeString(): String {
    val totalSeconds = this / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

inline fun Long.toTimeString():String = this.toInt().toTimeString()


internal inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

internal fun TextView.setTextOrGone(message: CharSequence?) {
    visibility = if (message.isNullOrEmpty()) {
        View.GONE
    } else {
        View.VISIBLE
    }
    text = message
}
