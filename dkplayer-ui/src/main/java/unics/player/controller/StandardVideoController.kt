package unics.player.controller

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import unics.player.ScreenMode
import unics.player.UCSPManager
import unics.player.control.*
import unics.player.control.GestureControlComponent
import unics.player.control.internal.toast
import unics.player.internal.UCSPUtil
import unics.player.kernel.UCSPlayer
import xyz.doikki.dkplayer.ui.UNDEFINED_LAYOUT
import xyz.doikki.videocontroller.R
import xyz.doikki.videocontroller.component.LiveControlView

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

    protected val mLockView: View
    protected val mLoadingView: ProgressBar?
    private var mBuffering = false

    /**
     * 是否启用Lock逻辑
     */
    var enableLock: Boolean = !UCSPManager.isTelevisionUiMode

    /**
     * 快速添加各个组件
     * @param title  标题
     * @param isLive 是否为直播
     */
    fun addDefaultControlComponent(title: String?, isLive: Boolean) {
        val completeView = CompleteControlComponent(context)
        val errorView = ErrorControlComponent(context)
        val prepareView = PrepareControlComponent(context)
        prepareView.setClickStart()
        val titleView = TitleBarControlComponent(context)
        titleView.setTitle(title)
        addControlComponent(completeView, errorView, prepareView, titleView)
        if (isLive) {
            addControlComponent(LiveControlView(context))
        } else {
            addControlComponent(VodControlComponent(context))
        }
        addControlComponent(GestureControlComponent(context))
        seekEnabled = !isLive
    }

    protected open fun onLockClick(v: View) {
        toggleLock()
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (enableLock) {
            if (isLocked) {
                mLockView.isSelected = true
                toast(R.string.dkplayer_locked)
            } else {
                mLockView.isSelected = false
                toast(R.string.dkplayer_unlocked)
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!enableLock)
            return

        if (isFullScreen) {
            if (isVisible) {
                if (mLockView.visibility == GONE) {
                    mLockView.visibility = VISIBLE
                    if (anim != null) {
                        mLockView.startAnimation(anim)
                    }
                }
            } else {
                mLockView.visibility = GONE
                if (anim != null) {
                    mLockView.startAnimation(anim)
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
                mLockView.visibility = GONE
            }
            ScreenMode.FULL_SCREEN -> if (isShowing) {
                mLockView.visibility = VISIBLE
            } else {
                mLockView.visibility = GONE
            }
        }

        invokeOnContainerAttached(false) {
            val activity = mActivity ?: return
            if (it.hasCutout()) {
                val orientation = activity.requestedOrientation
                val dp24 = UCSPUtil.dpInt(context, 24)
                val cutoutHeight = it.getCutoutHeight()
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        val lblp = mLockView.layoutParams as LayoutParams
                        lblp.setMargins(dp24, 0, dp24, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        val layoutParams = mLockView.layoutParams as LayoutParams
                        layoutParams.setMargins(dp24 + cutoutHeight, 0, dp24 + cutoutHeight, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        val layoutParams = mLockView.layoutParams as LayoutParams
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
                mLockView.isSelected = false
                mLoadingView?.visibility = GONE
            }
            UCSPlayer.STATE_PLAYING, UCSPlayer.STATE_PAUSED, UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR, UCSPlayer.STATE_BUFFERED -> {
                if (playState == UCSPlayer.STATE_BUFFERED) {
                    mBuffering = false
                }
                if (!mBuffering) {
                    mLoadingView?.visibility = GONE
                }
            }
            UCSPlayer.STATE_PREPARING, UCSPlayer.STATE_BUFFERING -> {
                mLoadingView?.visibility = VISIBLE
                if (playState == UCSPlayer.STATE_BUFFERING) {
                    mBuffering = true
                }
            }
            UCSPlayer.STATE_PLAYBACK_COMPLETED -> {
                mLoadingView?.visibility = GONE
                mLockView.visibility = GONE
                mLockView.isSelected = false
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

    init {
        View.inflate(
            context,
            if (layoutId > 0) layoutId else R.layout.ucsp_ctrl_standard_controller,
            this
        )
        mLockView = findViewById(R.id.ucsp_ctrl_lock)
        mLockView.setOnClickListener(::onLockClick)
        mLoadingView = findViewById(R.id.ucsp_ctrl_loading)
    }
}