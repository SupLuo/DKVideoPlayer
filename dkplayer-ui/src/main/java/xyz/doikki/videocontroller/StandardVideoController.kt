package xyz.doikki.videocontroller

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import unics.player.kernel.UCSPlayer
import xyz.doikki.dkplayer.ui.UNDEFINED_LAYOUT
import xyz.doikki.videocontroller.component.*
import unics.player.UCSPManager
import unics.player.internal.UCSPUtil
import droid.unicstar.player.ui.TVCompatible
import droid.unicstar.player.ui.toast
import unics.player.ScreenMode
import unics.player.controller.GestureMediaController

/**
 * 直播/点播控制器
 * 注意：此控制器仅做一个参考，如果想定制ui，你可以直接继承GestureVideoController或者BaseVideoController实现
 * 你自己的控制器
 * Created by Doikki on 2017/4/7.
 */
@TVCompatible(message = "TV上使用不提供lock相关的逻辑")
open class StandardVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : GestureMediaController(context, attrs) {

    protected val lockButton: View
    protected val loadingIndicator: ProgressBar?
    private var isBuffering = false

    var enableLock: Boolean = !UCSPManager.isTelevisionUiMode

    init {
        if (layoutId > 0)
            inflate(context, layoutId, this)
        else {
            inflate(context, R.layout.dkplayer_layout_standard_controller, this)
        }
        lockButton = findViewById(R.id.lock)
        lockButton.setOnClickListener(::onLockClick)
        loadingIndicator = findViewById(R.id.loading)
    }

    /**
     * 快速添加各个组件
     * @param title  标题
     * @param isLive 是否为直播
     */
    fun addDefaultControlComponent(title: String?, isLive: Boolean) {
        val completeView = CompleteView(context)
        val errorView = ErrorView(context)
        val prepareView = PrepareView(context)
        prepareView.setClickStart()
        val titleView = TitleView(context)
        titleView.setTitle(title)
        addControlComponent(completeView, errorView, prepareView, titleView)
        if (isLive) {
            addControlComponent(LiveControlView(context))
        } else {
            addControlComponent(VodControlView(context))
        }
        addControlComponent(GestureView(context))
        seekEnabled = !isLive
    }

    protected open fun onLockClick(v: View) {
        toggleLock()
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (enableLock) {
            if (isLocked) {
                lockButton.isSelected = true
                toast(R.string.dkplayer_locked)
            } else {
                lockButton.isSelected = false
                toast(R.string.dkplayer_unlocked)
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!enableLock)
            return
        if (isFullScreen) {
            if (isVisible) {
                if (lockButton.visibility == GONE) {
                    lockButton.visibility = VISIBLE
                    if (anim != null) {
                        lockButton.startAnimation(anim)
                    }
                }
            } else {
                lockButton.visibility = GONE
                if (anim != null) {
                    lockButton.startAnimation(anim)
                }
            }
        }
    }

    override fun onScreenModeChanged(screenMode: Int) {
        super.onScreenModeChanged(screenMode)
        if (!enableLock)
            return
        when (screenMode) {
            ScreenMode.NORMAL -> {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                lockButton.visibility = GONE
            }
            ScreenMode.FULL_SCREEN -> if (isShowing) {
                lockButton.visibility = VISIBLE
            } else {
                lockButton.visibility = GONE
            }
        }

        invokeOnContainerAttached(false) {
            val activity = mActivity ?: return
            if (it.hasCutout()) {
                val orientation = activity.requestedOrientation
                val dp24 = UCSPUtil.dpInt(context,24)
                val cutoutHeight = it.getCutoutHeight()
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        val lblp = lockButton.layoutParams as LayoutParams
                        lblp.setMargins(dp24, 0, dp24, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        val layoutParams = lockButton.layoutParams as LayoutParams
                        layoutParams.setMargins(dp24 + cutoutHeight, 0, dp24 + cutoutHeight, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        val layoutParams = lockButton.layoutParams as LayoutParams
                        layoutParams.setMargins(dp24, 0, dp24, 0)
                    }
                }
            }
        }

    }

    override fun onPlayerStateChanged(playState: Int) {
        super.onPlayerStateChanged(playState)
        when (playState) {
            UCSPlayer.STATE_IDLE -> {
                lockButton.isSelected = false
                loadingIndicator?.visibility = GONE
            }
            UCSPlayer.STATE_PLAYING, UCSPlayer.STATE_PAUSED, UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR, UCSPlayer.STATE_BUFFERED -> {
                if (playState == UCSPlayer.STATE_BUFFERED) {
                    isBuffering = false
                }
                if (!isBuffering) {
                    loadingIndicator?.visibility = GONE
                }
            }
            UCSPlayer.STATE_PREPARING, UCSPlayer.STATE_BUFFERING -> {
                loadingIndicator?.visibility = VISIBLE
                if (playState == UCSPlayer.STATE_BUFFERING) {
                    isBuffering = true
                }
            }
            UCSPlayer.STATE_PLAYBACK_COMPLETED -> {
                loadingIndicator?.visibility = GONE
                lockButton.visibility = GONE
                lockButton.isSelected = false
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (isLocked) {
            show()
            toast(R.string.dkplayer_lock_tip)
            return true
        }
        return if (isFullScreen) {
            stopFullScreen()
        } else {
            super.onBackPressed()
        }
//        return if (controlWrapper!!.isFullScreen) {
//            stopFullScreen()
//        } else super.onBackPressed()
    }
}