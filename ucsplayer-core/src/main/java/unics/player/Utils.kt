package unics.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import unics.player.internal.ploge
import unics.player.kernel.UCSPlayer

@PublishedApi
internal const val TAG = "UCSPlayer"


inline fun Boolean?.orDefault(def: Boolean = false): Boolean {
    return this ?: def
}

inline fun Int?.orDefault(def: Int = 0) = this ?: def
internal inline fun Float?.orDefault(def: Float = 0f) = this ?: def
internal inline fun Long?.orDefault(def: Long = 0) = this ?: def
internal inline fun Double?.orDefault(def: Double = 0.0) = this ?: def
internal inline fun <T> T?.orDefault(default: T): T = this ?: default
internal inline fun <T> T?.orDefault(initializer: () -> T): T = this ?: initializer()
inline fun <reified K> Map<*, *>.loopKeyWhen(block: (K) -> Unit) {
    for ((key) in this) {
        if (key is K) {
            block(key)
        }
    }
}

internal inline fun <reified V> Map<*, *>.loopValueWhen(block: (V) -> Unit) {
    for ((_, value) in this) {
        if (value is V) {
            block(value)
        }
    }
}

internal inline fun <K> MutableMap<K, *>.removeAllByKey(block: (K) -> Boolean) {
    val it: MutableIterator<Map.Entry<K, *>> = this.iterator()
    while (it.hasNext()) {
        val (key, _) = it.next()
        if (block(key)) {
            it.remove()
        }
    }
}

internal inline fun <V> MutableMap<*, V>.removeAllByValue(filter: (V) -> Boolean) {
    val it: MutableIterator<Map.Entry<*, V>> = this.iterator()
    while (it.hasNext()) {
        val (_, value) = it.next()
        if (filter(value)) {
            it.remove()
        }
    }
}

inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

/**
 * 从Parent中移除自己
 */
internal inline fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

internal inline val Activity.decorView: ViewGroup? get() = window.decorView as? ViewGroup
internal inline val Activity.contentView: ViewGroup? get() = findViewById(android.R.id.content)

internal inline val Context.layoutInflater: LayoutInflater get() = LayoutInflater.from(this)
internal inline val View.layoutInflater: LayoutInflater get() = context.layoutInflater

@PublishedApi
internal inline fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

inline fun Context.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageId, length).show()
}

@PublishedApi
internal inline fun View.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    context.toast(message, length)
}

inline fun View.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    context.toast(messageId, length)
}

fun TextView.setTextOrGone(message: CharSequence?) {
    visibility = if (message.isNullOrEmpty()) {
        View.GONE
    } else {
        View.VISIBLE
    }
    text = message
}

/**
 * 是否是第一次按下按键
 */
internal val KeyEvent.isUniqueDown: Boolean get() = action == KeyEvent.ACTION_DOWN && repeatCount == 0
internal const val INVALIDATE_SEEK_POSITION = -1

/**
 * 能否获取焦点
 */
internal val View.canTakeFocus: Boolean
    get() = isFocusable && this.visibility == View.VISIBLE && isEnabled

/**
 * Returns a string containing player state debugging information.
 */
internal fun screenMode2str(@ScreenMode mode: Int): String? {
    val playerStateString: String = when (mode) {
        UCSVideoView.SCREEN_MODE_NORMAL -> "normal"
        UCSVideoView.SCREEN_MODE_FULL -> "full screen"
        UCSVideoView.SCREEN_MODE_TINY -> "tiny screen"
        else -> "normal"
    }
    return String.format("screenMode: %s", playerStateString)
}

/**
 * Returns a string containing player state debugging information.
 */
internal fun playState2str(state: Int): String? {
    val playStateString: String = when (state) {
        UCSPlayer.STATE_IDLE -> "idle"
        UCSPlayer.STATE_PREPARING -> "preparing"
        UCSPlayer.STATE_PREPARED -> "prepared"
        UCSPlayer.STATE_PLAYING -> "playing"
        UCSPlayer.STATE_PAUSED -> "pause"
        UCSPlayer.STATE_BUFFERING -> "buffering"
        UCSPlayer.STATE_BUFFERED -> "buffered"
        UCSPlayer.STATE_PLAYBACK_COMPLETED -> "playback completed"
        UCSPlayer.STATE_ERROR -> "error"
        else -> "idle"
    }
    return String.format("playState: %s", playStateString)
}