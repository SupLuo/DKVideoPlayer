package unics.player.internal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.TypedValue
import android.view.MotionEvent
import unics.player.ScreenMode
import unics.player.UCSVideoView
import unics.player.kernel.UCSPlayer

/**
 * 采用工具类而不采用扩展工具函数，主要是不想因为本库的扩展影响用户的工程（虽然可以通过jvm name修改生成的文件名字，但是堆用户工程的kt文件使用仍然还是有影响）
 */
object UCSPUtil {

    /**
     * 从上下文中获取[Activity]
     */
    @JvmStatic
    fun getActivityContext(context: Context): Activity? {
        var tmpCtx: Context? = context
        while (tmpCtx is ContextWrapper) {
            if (tmpCtx is Activity) {
                return tmpCtx
            }
            tmpCtx = tmpCtx.baseContext
        }
        return null
    }

    /**
     * Returns a string containing player state debugging information.
     */
    @JvmStatic
    fun screenMode2str(@ScreenMode mode: Int): String {
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
    @JvmStatic
    fun playState2str(state: Int): String {
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

    @JvmStatic
    inline fun dpInt(context: Context, value: Int): Int = dpInt(context, value.toFloat())

    @JvmStatic
    inline fun dpInt(context: Context, value: Float): Int =
        unitValue(context, TypedValue.COMPLEX_UNIT_DIP, value).toInt()

    @JvmStatic
    inline fun unitValue(context: Context, unit: Int, value: Float): Float =
        TypedValue.applyDimension(unit, value, context.resources.displayMetrics)

    /**
     * 边缘检测
     */
    @JvmStatic
    fun isEdge(context: Context, e: MotionEvent): Boolean {
        val edgeSize = dpInt(context, 40)
        return e.rawX < edgeSize
                || e.rawX > getScreenWidth(context, true) - edgeSize
                || e.rawY < edgeSize
                || e.rawY > getScreenHeight(context, true) - edgeSize
    }

}