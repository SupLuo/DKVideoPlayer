package xyz.doikki.videocontroller.component

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import xyz.doikki.videoplayer.DKPlayerConfig
import xyz.doikki.videoplayer.DKVideoView
import xyz.doikki.videoplayer.TVCompatible

/**
 * 准备播放界面:包含加载中、封面、网络提示
 */
@TVCompatible
class PrepareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = R.layout.dkplayer_layout_prepare_view
) : BaseControlComponent(context, attrs, defStyleAttr) {

    /**
     * 缩略图
     */
    private val mThumb: ImageView

    /**
     * 开始播放
     */
    private val mStartPlay: ImageView

    /**
     * 正在加载
     */
    private val mLoading: ProgressBar

    /**
     * 网络提醒
     */
    private val mNetWarning: FrameLayout

    /**
     * 设置点击此界面开始播放
     */
    fun setClickStart() {
//        if (isTelevisionUiMode()) {
//            setViewInFocusMode(this)
//        }
        setOnClickListener { controller?.playerControl?.start() }
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
            DKVideoView.STATE_START_ABORT -> {
                visibility = VISIBLE
                mNetWarning.visibility = VISIBLE
                mNetWarning.bringToFront()
            }
        }
    }

    init {
        layoutInflater.inflate(layoutId, this)
        mThumb = findViewById(R.id.thumb)
        mStartPlay = findViewById(R.id.start_play)
        mLoading = findViewById(R.id.loading)
        mNetWarning = findViewById(R.id.net_warning_layout)
        val btnInWarning = findViewById<View>(R.id.status_btn)
        if (isTelevisionUiMode()) {
            setViewInFocusMode(mStartPlay)
            setViewInFocusMode(btnInWarning)
        }
        btnInWarning.setOnClickListener {
            mNetWarning.visibility = GONE
            DKPlayerConfig.isPlayOnMobileNetwork = true
            controller?.playerControl?.start()
        }
    }
}