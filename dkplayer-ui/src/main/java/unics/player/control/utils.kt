package unics.player.control

import android.content.Context
import android.util.TypedValue

internal inline fun Context.dip(value: Int): Float = unitValue(TypedValue.COMPLEX_UNIT_DIP, value.toFloat())
internal inline fun Context.sp(value: Int): Float = unitValue(TypedValue.COMPLEX_UNIT_SP, value.toFloat())

internal inline fun Context.unitValue(unit: Int, value: Float): Float =
    TypedValue.applyDimension(unit, value, this.resources.displayMetrics)