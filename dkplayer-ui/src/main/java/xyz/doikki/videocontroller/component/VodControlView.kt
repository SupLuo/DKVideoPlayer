package xyz.doikki.videocontroller.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import droid.unicstar.player.UCSVideoView
import xyz.doikki.videoplayer.TVCompatible
import xyz.doikki.videoplayer.util.PlayerUtils
import droid.unicstar.player.orDefault
import droid.unicstar.player.player.UCSPlayer

/**
 * 点播底部控制栏
 */
@TVCompatible(message = "TV上不显示全屏按钮")
open class VodControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr) {

    private var mBottomContainer: LinearLayout? = null
    private val mFullScreen: ImageView?
    private var mTotalTime: TextView? = null
    private var mCurrTime: TextView? = null
    private var mPlayButton: ImageView? = null
    private var mVideoProgress: SeekBar? = null
    private var mBottomProgress: ProgressBar? = null

    /**
     * 是否正在拖动SeekBar
     */
    private var mTrackingTouch = false

    /**
     * 是否显示底部进度条，默认显示
     */
    var showBottomProgress = true

    private val innerViewClick: OnClickListener = OnClickListener {
        when (it.id) {
            R.id.fullscreen -> {
                toggleFullScreen()
            }
            R.id.iv_play -> {
                mController?.togglePlay()
            }
        }
    }

    private val innerSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser)
                return

            mController?.playerControl?.let { player ->
                val duration = player.getDuration()
                val newPosition = duration * progress / seekBar.max.orDefault(100)
                mCurrTime?.text = PlayerUtils.stringForTime(newPosition.toInt())
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            mTrackingTouch = true
            mController?.let {
                it.stopUpdateProgress()
                it.stopFadeOut()
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            try {
                mController?.let { controller ->
                    val player = this@VodControlView.playerControl ?: return@let
                    val duration = player.getDuration()
                    val newPosition = duration * seekBar.progress / seekBar.max
                    player.seekTo(newPosition.toInt().toLong())
                    mTrackingTouch = false
                    controller.startUpdateProgress()
                    controller.startFadeOut()
                }
            } finally {
                mTrackingTouch = false
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            mBottomContainer?.let { bottomContainer ->
                bottomContainer.visibility = VISIBLE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }
            if (showBottomProgress) {
                mBottomProgress?.visibility = GONE
            }
        } else {
            mBottomContainer?.let { bottomContainer ->
                bottomContainer.visibility = GONE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }

            if (showBottomProgress) {
                mBottomProgress?.let { bottomProgress ->
                    bottomProgress.visibility = VISIBLE
                    val animation = AlphaAnimation(0f, 1f)
                    animation.duration = 300
                    bottomProgress.startAnimation(animation)
                }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            UCSPlayer.STATE_IDLE, UCSPlayer.STATE_PLAYBACK_COMPLETED -> {
                visibility = GONE
                mBottomProgress?.let {
                    it.progress = 0
                    it.secondaryProgress = 0
                }
                mVideoProgress?.let {
                    it.progress = 0
                    it.secondaryProgress = 0
                }
            }
            UCSPlayer.STATE_PREPARED_BUT_ABORT, UCSPlayer.STATE_PREPARING,
            UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR -> visibility = GONE
            UCSPlayer.STATE_PLAYING -> {
                mPlayButton?.isSelected = true
                if (showBottomProgress) {
                    if (mController?.isShowing.orDefault()) {
                        mBottomProgress?.visibility = GONE
                        mBottomContainer?.visibility = VISIBLE
                    } else {
                        mBottomContainer?.visibility = GONE
                        mBottomProgress?.visibility = VISIBLE
                    }
                } else {
                    mBottomContainer?.visibility = GONE
                }
                visibility = VISIBLE
                //开始刷新进度
                mController?.startUpdateProgress()
            }
            UCSPlayer.STATE_PAUSED -> mPlayButton?.isSelected = false
            UCSPlayer.STATE_BUFFERING -> {
                mPlayButton?.isSelected = playerControl?.isPlaying().orDefault()
                // 停止刷新进度
                mController?.stopUpdateProgress()
            }
            UCSPlayer.STATE_BUFFERED -> {
                mPlayButton?.isSelected = playerControl?.isPlaying().orDefault()
                //开始刷新进度
                mController?.startUpdateProgress()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onScreenModeChanged(screenMode: Int) {
        when (screenMode) {
            UCSVideoView.SCREEN_MODE_NORMAL -> mFullScreen?.isSelected = false
            UCSVideoView.SCREEN_MODE_FULL -> mFullScreen?.isSelected = true
        }

        val activity = this.activity ?: return
        val bottomContainer = mBottomContainer
        val bottomProgress = mBottomProgress

        //底部容器和进度都为空，则不用处理后续逻辑
        if (bottomContainer == null && bottomProgress == null)
            return
        containerControl?.let {
            player->
            if (player.hasCutout()) {
                val orientation = activity.requestedOrientation
                val cutoutHeight = player.getCutoutHeight()
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        bottomContainer?.setPadding(0, 0, 0, 0)
                        bottomProgress?.setPadding(0, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        bottomContainer?.setPadding(cutoutHeight, 0, 0, 0)
                        bottomProgress?.setPadding(cutoutHeight, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        bottomContainer?.setPadding(0, 0, cutoutHeight, 0)
                        bottomProgress?.setPadding(0, 0, cutoutHeight, 0)
                    }
                }
            }
        }

    }


    override fun onLockStateChanged(isLocked: Boolean) {
        onVisibilityChanged(!isLocked, null)
    }

    /**
     * 横竖屏切换
     */
    private fun toggleFullScreen() {
        mController?.toggleFullScreen()
        // 下面方法会根据适配宽高决定是否旋转屏幕
//        mControlWrapper.toggleFullScreenByVideoSize(activity);
    }

    override fun onProgressChanged(duration: Int, position: Int) {
        if (mTrackingTouch) {
            return
        }
        mVideoProgress?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                val pos = (position * 1.0 / duration * seekBar.max).toInt()
                seekBar.progress = pos
                mBottomProgress?.progress = pos
            } else {
                seekBar.isEnabled = false
            }
            val percent = playerControl?.getBufferedPercentage().orDefault()
            if (percent >= 95) { //解决缓冲进度不能100%问题
                seekBar.secondaryProgress = seekBar.max
                mBottomProgress?.secondaryProgress = mBottomProgress?.max.orDefault(100)
            } else {
                seekBar.secondaryProgress = percent * 10
                mBottomProgress?.secondaryProgress = percent * 10
            }
        }

        mTotalTime?.text = PlayerUtils.stringForTime(duration)
        mCurrTime?.text = PlayerUtils.stringForTime(position)
    }

    init {
        visibility = GONE
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.dkplayer_layout_vod_control_view, this)
        }

        mFullScreen = findViewById(R.id.fullscreen)
        mFullScreen?.setOnClickListener(innerViewClick)
        if (isTelevisionUiMode()) {//tv 模式不会显示全屏按钮
            mFullScreen?.visibility = View.GONE
        }

        mBottomContainer = findViewById(R.id.bottom_container)
        mVideoProgress = findViewById<SeekBar?>(R.id.seekBar)?.also {
            it.setOnSeekBarChangeListener(innerSeekBarChangeListener)
            //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
            if (Build.VERSION.SDK_INT <= 22) {
                it.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        mTotalTime = findViewById(R.id.total_time)
        mCurrTime = findViewById(R.id.curr_time)
        mPlayButton = findViewById(R.id.iv_play)
        mPlayButton?.setOnClickListener(innerViewClick)
        mBottomProgress = findViewById(R.id.bottom_progress)
    }
}