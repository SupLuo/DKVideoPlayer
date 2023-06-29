package unics.player.control

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import droid.unicstar.player.ui.toTimeString
import unics.player.controller.GestureControlComponent
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

/**
 * 手势控制：用于手势处理亮度、进度、音量等
 */
class GestureControlComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr),
    GestureControlComponent {

    private val mContentContainer: ViewGroup
    private val mImageView: ImageView
    private val mProgress: ProgressBar
    private val mTipText: TextView

    override fun onStartSlide() {
        mController?.hide()
        mContentContainer.visibility = VISIBLE
        mContentContainer.alpha = 1f
    }

    override fun onStopSlide() {
        mContentContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mContentContainer.visibility = GONE
                }
            })
            .start()
    }

    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        mProgress.visibility = GONE
        if (slidePosition > currentPosition) {
            mImageView.setImageResource(R.drawable.dkplayer_ic_action_fast_forward)
        } else if (slidePosition < currentPosition) {
            mImageView.setImageResource(R.drawable.dkplayer_ic_action_fast_rewind)
        } else {
            //相等的情况不处理，避免最大最小位置图标错乱
        }
        mTipText.text =
            "${slidePosition.toTimeString()}/${duration.toTimeString()}"
    }

    override fun onBrightnessChange(percent: Int) {
        mProgress.visibility = VISIBLE
        mImageView.setImageResource(R.drawable.dkplayer_ic_action_brightness)
        mTipText.text = "$percent%"
        mProgress.progress = percent
    }

    override fun onVolumeChange(percent: Int) {
        mProgress.visibility = VISIBLE
        if (percent <= 0) {
            mImageView.setImageResource(R.drawable.dkplayer_ic_action_volume_off)
        } else {
            mImageView.setImageResource(R.drawable.dkplayer_ic_action_volume_up)
        }
        mTipText.text = "$percent%"
        mProgress.progress = percent
    }

    override fun onPlayStateChanged(playState: Int) {
        visibility =
            if (playState == UCSPlayer.STATE_IDLE
                || playState == UCSPlayer.STATE_PREPARED_BUT_ABORT
                || playState == UCSPlayer.STATE_PREPARING
                || playState == UCSPlayer.STATE_PREPARED
                || playState == UCSPlayer.STATE_ERROR
                || playState == UCSPlayer.STATE_PLAYBACK_COMPLETED
            ) {
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
            layoutInflater.inflate(R.layout.ucsp_ctrl_gesture_control_component, this)
        }
        mImageView = findViewById(R.id.ucsp_ctrl_image)
        mProgress = findViewById(R.id.ucsp_ctrl_progress)
        mTipText = findViewById(R.id.ucsp_ctrl_text)
        mContentContainer = findViewById(R.id.ucsp_ctrl_gestureCtrlContainer)
    }
}