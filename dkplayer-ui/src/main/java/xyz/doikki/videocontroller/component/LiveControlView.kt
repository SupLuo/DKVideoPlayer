package xyz.doikki.videocontroller.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import unics.player.ScreenMode
import unics.player.control.BaseControlComponent
import unics.player.internal.UCSPUtil

import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

/**
 * 直播底部控制栏
 *
 *
 * 此控件不适配TV模式
 */
class LiveControlView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseControlComponent(context, attrs, defStyleAttr), View.OnClickListener {

    private val mFullScreen: ImageView
    private val mBottomContainer: LinearLayout
    private val mPlayButton: ImageView

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            if (visibility == GONE) {
                visibility = VISIBLE
                anim?.let { startAnimation(it) }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                anim?.let { startAnimation(it) }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            UCSPlayer.STATE_IDLE, UCSPlayer.STATE_PREPARED_BUT_ABORT, UCSPlayer.STATE_PREPARING, UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR, UCSPlayer.STATE_PLAYBACK_COMPLETED -> visibility =
                GONE
            UCSPlayer.STATE_PLAYING -> mPlayButton.isSelected = true
            UCSPlayer.STATE_PAUSED -> mPlayButton.isSelected = false
            UCSPlayer.STATE_BUFFERING, UCSPlayer.STATE_BUFFERED -> mPlayButton.isSelected =
                mController?.playerControl?.isPlaying() ?: false
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onScreenModeChanged(screenMode: Int) {
        when (screenMode) {
            ScreenMode.NORMAL -> mFullScreen.isSelected = false
            ScreenMode.FULL_SCREEN -> mFullScreen.isSelected = true
        }
        val activity = UCSPUtil.getActivityContext(context) ?: return
        containerControl?.let { containerControl ->
            if (containerControl.hasCutout()) {
                val orientation = activity.requestedOrientation
                val cutoutHeight = containerControl.getCutoutHeight()
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        mBottomContainer.setPadding(0, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        mBottomContainer.setPadding(cutoutHeight, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        mBottomContainer.setPadding(0, 0, cutoutHeight, 0)
                    }
                }
            }
        }

    }

    override fun onLockStateChanged(isLocked: Boolean) {
        onVisibilityChanged(!isLocked, null)
    }

    override fun onClick(v: View) {
        mController?.let { controller ->
            when (v.id) {
                R.id.fullscreen -> {
                    controller.toggleFullScreen()
                }
                R.id.iv_play -> {
                    controller.togglePlay()
                }
                R.id.iv_refresh -> {
                    controller.replay(true)
                }
                else -> {}
            }
        }

    }

    init {
        visibility = GONE
        layoutInflater.inflate(R.layout.dkplayer_layout_live_control_view, this)
        mFullScreen = findViewById(R.id.fullscreen)
        mFullScreen.setOnClickListener(this)
        mBottomContainer = findViewById(R.id.bottom_container)
        mPlayButton = findViewById(R.id.iv_play)
        mPlayButton.setOnClickListener(this)
        val refresh = findViewById<ImageView>(R.id.iv_refresh)
        refresh.setOnClickListener(this)
    }
}