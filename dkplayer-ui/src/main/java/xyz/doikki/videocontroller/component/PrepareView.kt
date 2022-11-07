package xyz.doikki.videocontroller.component

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.DKVideoView
import xyz.doikki.videoplayer.TVCompatible

/**
 * 准备播放界面
 */
@TVCompatible(message = "不用做额外适配")
class PrepareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr) {

    private val mThumb: ImageView
    private val mStartPlay: ImageView
    private val mLoading: ProgressBar
    private val mNetWarning: FrameLayout

    /**
     * 封面ImageView
     */
    val coverImage: ImageView? get() = mThumb

    /**
     * 设置点击此界面开始播放
     */
    fun setClickStart() {
        setOnClickListener { mController?.playerControl?.start() }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            DKVideoView.STATE_PREPARING -> {
                bringToFront()
                visibility = VISIBLE
                mStartPlay.visibility = GONE
                mNetWarning.visibility = GONE
                mLoading.visibility = VISIBLE
            }
            DKVideoView.STATE_PLAYING, DKVideoView.STATE_PAUSED, DKVideoView.STATE_ERROR, DKVideoView.STATE_BUFFERING, DKVideoView.STATE_BUFFERED, DKVideoView.STATE_PLAYBACK_COMPLETED ->
                visibility = GONE
            DKVideoView.STATE_IDLE -> {
                visibility = VISIBLE
                bringToFront()
                mLoading.visibility = GONE
                mNetWarning.visibility = GONE
                mStartPlay.visibility = VISIBLE
                mThumb.visibility = VISIBLE
            }
            DKVideoView.STATE_PREPARED_BUT_ABORT -> {
                visibility = VISIBLE
                mNetWarning.visibility = VISIBLE
                mNetWarning.bringToFront()
            }
        }
    }

    init {
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.dkplayer_layout_prepare_view, this)
        }
        mThumb = findViewById(R.id.thumb)
        mStartPlay = findViewById(R.id.start_play)
        mLoading = findViewById(R.id.loading)
        mNetWarning = findViewById(R.id.net_warning_layout)
        findViewById<View?>(R.id.status_btn)?.setOnClickListener {
            mNetWarning.visibility = GONE
            DKManager.isPlayOnMobileNetwork = true
            mController?.playerControl?.start()
        }
    }
}