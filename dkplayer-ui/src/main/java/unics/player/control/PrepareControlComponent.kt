package unics.player.control

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import unics.player.UCSPManager
import unics.player.kernel.UCSPlayer
import unics.player.control.TVCompatible
import unics.player.control.BaseControlComponent

/**
 * 准备播放界面
 */
@TVCompatible(message = "不用做额外适配")
class PrepareControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr) {

    private val mStartPlay: ImageView
    private val mLoading: ProgressBar

    /**
     * 设置点击此界面开始播放
     */
    fun setClickStart() {
        setOnClickListener { mController?.playerControl?.start() }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            UCSPlayer.STATE_PREPARING -> {
                bringToFront()
                visibility = VISIBLE
                mStartPlay.visibility = GONE
                mLoading.visibility = VISIBLE
            }
            UCSPlayer.STATE_PLAYING, UCSPlayer.STATE_PAUSED, UCSPlayer.STATE_ERROR, UCSPlayer.STATE_BUFFERING, UCSPlayer.STATE_BUFFERED, UCSPlayer.STATE_PLAYBACK_COMPLETED ->
                visibility = GONE
            UCSPlayer.STATE_IDLE -> {
                visibility = VISIBLE
                bringToFront()
                mLoading.visibility = GONE
                mStartPlay.visibility = VISIBLE
            }
            UCSPlayer.STATE_PREPARED_BUT_ABORT -> {
                visibility = VISIBLE
            }
        }
    }

    init {
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.dkplayer_layout_prepare_view, this)
        }
        mStartPlay = findViewById(R.id.ucsp_ctrl_play)
        mLoading = findViewById(R.id.ucsp_ctrl_loading)

    }
}