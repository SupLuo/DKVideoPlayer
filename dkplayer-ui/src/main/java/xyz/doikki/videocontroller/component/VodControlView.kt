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
import droid.unicstar.player.ui.TVCompatible
import droid.unicstar.player.ui.toTimeString
import unics.player.ScreenMode
import unics.player.control.widget.FloatTextIndicatorSeekBar
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

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

    private var mVodCtrlContainer: LinearLayout? = null
    private var mDurationView: TextView? = null
    private var mTimeView: TextView? = null
    private val mFullScreenView: ImageView?
    private var mPlayView: ImageView? = null
    private var mProgressBar: SeekBar? = null
    private var mTimeShiftBar:FloatTextIndicatorSeekBar? = null
    private var mTimeShiftBar2:FloatTextIndicatorSeekBar? = null
    private var mTimeShiftBar3:FloatTextIndicatorSeekBar? = null
    private var mProgressBarOverlay: ProgressBar? = null

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
            R.id.ucsp_ctrl_fullScreen -> {
                toggleFullScreen()
            }
            R.id.ucsp_ctrl_play -> {
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
                val newPosition = duration * progress / seekBar.max.coerceAtLeast(1)
                mTimeView?.text = newPosition.toTimeString()
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
            mVodCtrlContainer?.let { bottomContainer ->
                bottomContainer.visibility = VISIBLE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }
            if (showBottomProgress) {
                mProgressBarOverlay?.visibility = GONE
            }
        } else {
            mVodCtrlContainer?.let { bottomContainer ->
                bottomContainer.visibility = GONE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }

            if (showBottomProgress) {
                mProgressBarOverlay?.let { bottomProgress ->
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
                mProgressBarOverlay?.let {
                    it.progress = 0
                    it.secondaryProgress = 0
                }
                mProgressBar?.let {
                    it.progress = 0
                    it.secondaryProgress = 0
                }
                mTimeShiftBar?.let {
                    it.setProgress(0)
                    it.setSecondaryProgress(0)
                }
                mTimeShiftBar2?.let {
                    it.setProgress(0)
                    it.setSecondaryProgress(0)
                }
                mTimeShiftBar3?.let {
                    it.setProgress(0)
                    it.setSecondaryProgress(0)
                }
            }
            UCSPlayer.STATE_PREPARED_BUT_ABORT, UCSPlayer.STATE_PREPARING,
            UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR -> visibility = GONE
            UCSPlayer.STATE_PLAYING -> {
                mPlayView?.isSelected = true
                if (showBottomProgress) {
                    if (mController?.isShowing == true) {
                        mProgressBarOverlay?.visibility = GONE
                        mVodCtrlContainer?.visibility = VISIBLE
                    } else {
                        mVodCtrlContainer?.visibility = GONE
                        mProgressBarOverlay?.visibility = VISIBLE
                    }
                } else {
                    mVodCtrlContainer?.visibility = GONE
                }
                visibility = VISIBLE
                //开始刷新进度
                mController?.startUpdateProgress()
            }
            UCSPlayer.STATE_PAUSED -> mPlayView?.isSelected = false
            UCSPlayer.STATE_BUFFERING -> {
                mPlayView?.isSelected = playerControl?.isPlaying() ?: false
                // 停止刷新进度
                mController?.stopUpdateProgress()
            }
            UCSPlayer.STATE_BUFFERED -> {
                mPlayView?.isSelected = playerControl?.isPlaying() ?: false
                //开始刷新进度
                mController?.startUpdateProgress()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onScreenModeChanged(screenMode: Int) {
        when (screenMode) {
            ScreenMode.NORMAL -> mFullScreenView?.isSelected = false
            ScreenMode.FULL_SCREEN -> mFullScreenView?.isSelected = true
        }

        val activity = this.activity ?: return
        val bottomContainer = mVodCtrlContainer
        val bottomProgress = mProgressBarOverlay

        //底部容器和进度都为空，则不用处理后续逻辑
        if (bottomContainer == null && bottomProgress == null)
            return
        containerControl?.let { player ->
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
        mProgressBar?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                val pos = (position * 1.0 / duration * seekBar.max).toInt()
                seekBar.progress = pos
                mProgressBarOverlay?.progress = pos
            } else {
                seekBar.isEnabled = false
            }
            val percent = playerControl?.getBufferedPercentage() ?: 0
            if (percent >= 95) { //解决缓冲进度不能100%问题
                seekBar.secondaryProgress = seekBar.max
                mProgressBarOverlay?.secondaryProgress = mProgressBarOverlay?.max ?: 100
            } else {
                seekBar.secondaryProgress = percent * 10
                mProgressBarOverlay?.secondaryProgress = percent * 10
            }
        }

        mTimeShiftBar?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                val pos = (position * 1.0 / duration * seekBar.getMax()).toInt()
                seekBar.setProgress(pos)
                mProgressBarOverlay?.progress = pos
            } else {
                seekBar.isEnabled = false
            }
            val percent = playerControl?.getBufferedPercentage() ?: 0
            if (percent >= 95) { //解决缓冲进度不能100%问题
                seekBar.setSecondaryProgress(seekBar.getMax())
                mProgressBarOverlay?.secondaryProgress = mProgressBarOverlay?.max ?: 100
            } else {
                seekBar.setSecondaryProgress( percent * 10)
                mProgressBarOverlay?.secondaryProgress = percent * 10
            }
        }

        mTimeShiftBar2?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                val pos = (position * 1.0 / duration * seekBar.getMax()).toInt()
                seekBar.setProgress(pos)
            } else {
                seekBar.isEnabled = false
            }
            val percent = playerControl?.getBufferedPercentage() ?: 0
            if (percent >= 95) { //解决缓冲进度不能100%问题
                seekBar.setSecondaryProgress(seekBar.getMax())
            } else {
                seekBar.setSecondaryProgress( percent * 10)
            }
        }

        mTimeShiftBar3?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                val pos = (position * 1.0 / duration * seekBar.getMax()).toInt()
                seekBar.setProgress(pos)
            } else {
                seekBar.isEnabled = false
            }
            val percent = playerControl?.getBufferedPercentage() ?: 0
            if (percent >= 95) { //解决缓冲进度不能100%问题
                seekBar.setSecondaryProgress(seekBar.getMax())
            } else {
                seekBar.setSecondaryProgress( percent * 10)
            }
        }


        mDurationView?.text = duration.toTimeString()
        mTimeView?.text = position.toTimeString()
    }

    init {
        visibility = GONE
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.ucsp_ctrl_vod_control_layout, this)
        }

        mFullScreenView = findViewById(R.id.ucsp_ctrl_fullScreen)
        mFullScreenView?.setOnClickListener(innerViewClick)
        if (isTelevisionUiMode()) {//tv 模式不会显示全屏按钮
            mFullScreenView?.visibility = View.GONE
        }

        mVodCtrlContainer = findViewById(R.id.ucsp_ctrl_vodCtrlContainer)
        mProgressBar = findViewById<SeekBar?>(R.id.ucsp_ctrl_seekBar)?.also {
            it.setOnSeekBarChangeListener(innerSeekBarChangeListener)
            //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
            if (Build.VERSION.SDK_INT <= 22) {
                it.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        mTimeShiftBar = findViewById(R.id.ucsp_ctrl_seekBar2)
        mTimeShiftBar2 = findViewById(R.id.ucsp_ctrl_seekBar3)
        mTimeShiftBar3 = findViewById(R.id.ucsp_ctrl_seekBar4)
        mProgressBarOverlay = findViewById(R.id.ucsp_ctrl_progressOverlay)

        mDurationView = findViewById(R.id.ucsp_ctrl_duration)
        mTimeView = findViewById(R.id.ucsp_ctrl_time)
        mPlayView = findViewById(R.id.ucsp_ctrl_play)
        mPlayView?.setOnClickListener(innerViewClick)

    }
}