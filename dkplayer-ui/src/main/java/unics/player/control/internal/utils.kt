package unics.player.control

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal inline fun Context.dip(value: Int): Float =
    unitValue(TypedValue.COMPLEX_UNIT_DIP, value.toFloat())

internal inline fun Context.sp(value: Int): Float =
    unitValue(TypedValue.COMPLEX_UNIT_SP, value.toFloat())

internal inline fun Context.unitValue(unit: Int, value: Float): Float =
    TypedValue.applyDimension(unit, value, this.resources.displayMetrics)


internal inline fun Int?.orDefault(def: Int = 0):Int = this ?: def


@OptIn(ExperimentalContracts::class)
inline fun <R> TypedArray.use(block: (TypedArray) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this).also {
        recycle()
    }
}
