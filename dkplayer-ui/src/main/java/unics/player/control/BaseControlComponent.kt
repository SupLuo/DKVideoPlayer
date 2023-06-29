package unics.player.control

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import unics.player.control.internal.toast
import unics.player.controller.ControlComponent
import unics.player.controller.MediaController
import unics.player.controller.UCSContainerControl
import unics.player.internal.UCSPUtil
import unics.player.internal.plogw2
import unics.player.kernel.UCSPlayerControl

abstract class BaseControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ControlComponent {

    companion object {
        const val UNDEFINED_LAYOUT = -1
    }

    protected var mController: MediaController? = null

    val playerControl: UCSPlayerControl? get() = mController?.playerControl

    val containerControl: UCSContainerControl? get() = mController?.containerControl

    val isControllerShowing: Boolean get() = mController?.isShowing ?: false
//    /**
//     * 是否处于播放状态
//     *
//     * @return
//     */
//    protected val isInPlaybackState: Boolean
//    get() {
//
//    }
//        get() = mBindPlayer != null
//                && mPlayState != UCSPlayer.STATE_ERROR
//                && mPlayState != UCSPlayer.STATE_IDLE
//                && mPlayState != UCSPlayer.STATE_PREPARING
//                && mPlayState != UCSPlayer.STATE_PREPARED
//                && mPlayState != UCSPlayer.STATE_PREPARED_BUT_ABORT
//                && mPlayState != UCSPlayer.STATE_PLAYBACK_COMPLETED
//
//    protected val isInCompleteState: Boolean get() = mPlayState == UCSPlayer.STATE_PLAYBACK_COMPLETED
//
//    protected val isInErrorState: Boolean get() = mPlayState == UCSPlayer.STATE_ERROR

    protected val layoutInflater: LayoutInflater get() = LayoutInflater.from(context)

    protected val activity: Activity?
        get() = UCSPUtil.getActivityContext(context)

    protected fun setViewInFocusMode(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
    }

    override fun onControllerAttached(controller: MediaController) {
        mController = controller
    }

    override fun getView(): View? {
        return this
    }

    protected inline fun <R> invokeOnPlayerAttached(
        showToast: Boolean = true,
        block: (UCSPlayerControl) -> R
    ): R? {
        val player = playerControl
        if (player == null) {
            if (showToast) {
                toast("请先调用setMediaPlayer方法绑定播放器.")
            }
            plogw2("MediaController") {
                "error on ${Thread.currentThread().stackTrace[2].methodName} method invoke.but throwable is ignored."
            }
            return null
        }
        return block.invoke(player)
    }

    init {
        if (attrs == null && layoutParams == null) {
            //设置默认参数
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
}