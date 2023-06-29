package xyz.doikki.videocontroller.component

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import droid.unicstar.player.ui.toTimeString
import xyz.doikki.videocontroller.R
import unics.player.kernel.UCSPlayer
import droid.unicstar.player.ui.TVCompatible
import unics.player.controller.KeyControlComponent

/**
 * 手势控制：用于手势处理亮度、进度、音量等
 */
@TVCompatible(message = "已适配tv上进度操作的显示指示：亮度和音量一般在tv上不用处理")
class GestureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr), KeyControlComponent {

    private val mIcon: ImageView
    private val mProgressPercent: ProgressBar
    private val mTextPercent: TextView
    private val mCenterContainer: LinearLayout

    override fun onStartLeftOrRightKeyPressedForSeeking(event: KeyEvent) {
        onStartSlide()
    }

    override fun onStopLeftOrRightKeyPressedForSeeking(event: KeyEvent) {
        onStopSlide()
    }

    override fun onCancelLeftOrRightKeyPressedForSeeking(keyEvent: KeyEvent) {
        onStopSlide()
    }

    override fun onStartSlide() {
        mController?.hide()
        mCenterContainer.visibility = VISIBLE
        mCenterContainer.alpha = 1f
    }

    override fun onStopSlide() {
        mCenterContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mCenterContainer.visibility = GONE
                }
            })
            .start()
    }

    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        mProgressPercent.visibility = GONE
        if (slidePosition > currentPosition) {
            mIcon.setImageResource(R.drawable.dkplayer_ic_action_fast_forward)
        } else if (slidePosition < currentPosition) {
            mIcon.setImageResource(R.drawable.dkplayer_ic_action_fast_rewind)
        } else {
            //相等的情况不处理，避免最大最小位置图标错乱
        }
        mTextPercent.text =
            "${slidePosition.toTimeString()}/${duration.toTimeString()}"
    }

    override fun onBrightnessChange(percent: Int) {
        mProgressPercent.visibility = VISIBLE
        mIcon.setImageResource(R.drawable.dkplayer_ic_action_brightness)
        mTextPercent.text = "$percent%"
        mProgressPercent.progress = percent
    }

    override fun onVolumeChange(percent: Int) {
        mProgressPercent.visibility = VISIBLE
        if (percent <= 0) {
            mIcon.setImageResource(R.drawable.dkplayer_ic_action_volume_off)
        } else {
            mIcon.setImageResource(R.drawable.dkplayer_ic_action_volume_up)
        }
        mTextPercent.text = "$percent%"
        mProgressPercent.progress = percent
    }

    override fun onPlayStateChanged(playState: Int) {
        visibility =
            if (playState == UCSPlayer.STATE_IDLE || playState == UCSPlayer.STATE_PREPARED_BUT_ABORT || playState == UCSPlayer.STATE_PREPARING || playState == UCSPlayer.STATE_PREPARED || playState == UCSPlayer.STATE_ERROR || playState == UCSPlayer.STATE_PLAYBACK_COMPLETED) {
                GONE
            } else {
                VISIBLE
            }
    }

    init {
        visibility = GONE
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.dkplayer_layout_gesture_control_view, this)
        }
        mIcon = findViewById(R.id.iv_icon)
        mProgressPercent = findViewById(R.id.pro_percent)
        mTextPercent = findViewById(R.id.tv_percent)
        mCenterContainer = findViewById(R.id.center_container)
    }
}