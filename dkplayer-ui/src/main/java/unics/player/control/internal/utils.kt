package unics.player.control.internal

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerControl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal inline fun Context.dip(value: Int): Float =
    unitValue(TypedValue.COMPLEX_UNIT_DIP, value.toFloat())

internal inline fun Context.sp(value: Int): Float =
    unitValue(TypedValue.COMPLEX_UNIT_SP, value.toFloat())

internal inline fun Context.unitValue(unit: Int, value: Float): Float =
    TypedValue.applyDimension(unit, value, this.resources.displayMetrics)


internal inline fun Int?.orDefault(def: Int = 0): Int = this ?: def


@OptIn(ExperimentalContracts::class)
inline fun <R> TypedArray.use(block: (TypedArray) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this).also {
        recycle()
    }
}

@PublishedApi
internal inline fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

@PublishedApi
internal inline fun Context.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageId, length).show()
}

@PublishedApi
internal inline fun View.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    context.toast(message, length)
}

@PublishedApi
internal inline fun View.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    context.toast(messageId, length)
}

internal inline val UCSPlayerControl.isInPlaybackState:Boolean get() = this.currentState.isInPlaybackState

internal inline val UCSPlayerControl. isInCompleteState: Boolean get() = this.currentState == UCSPlayer.STATE_PLAYBACK_COMPLETED

internal inline val UCSPlayerControl.isInErrorState: Boolean get() = this.currentState == UCSPlayer.STATE_ERROR

internal val Int.isInPlaybackState
    get() = this != UCSPlayer.STATE_ERROR
            && this != UCSPlayer.STATE_IDLE
            && this != UCSPlayer.STATE_PREPARING
            && this != UCSPlayer.STATE_PREPARED
            && this != UCSPlayer.STATE_PREPARED_BUT_ABORT
            && this != UCSPlayer.STATE_PLAYBACK_COMPLETED


