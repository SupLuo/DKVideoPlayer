package unics.player.control

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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import unics.player.ScreenMode
import unics.player.control.widget.TimeFormatter
import unics.player.control.widget.TimeShiftBar
import unics.player.control.widget.TimeShiftFloatTextIndicatorSeekBar
import unics.player.control.widget.TimeShiftSeekBar
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

/**
 * 点播底部控制栏
 */
@TVCompatible(message = "TV上不显示全屏按钮")
open class VodControlComponent @JvmOverloads constructor(
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
    private var mTimeShiftBar: TimeShiftSeekBar? = null
    private var mTimeShiftSeekBarOverlay: TimeShiftBar? = null

    //是否正在拖动SeekBar
    protected var mTrackingTouch = false

    private var mTimeFormatter = TimeFormatter
    private var mTotalTimeInMs = Long.MIN_VALUE

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
                val duration = player.getDuration().toInt()
                val newPosition =  (progress.toDouble() / seekBar.max * duration).toInt()
                setProgress(newPosition,duration)
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
                    val player = this@VodControlComponent.playerControl ?: return@let
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

    @CallSuper
    override fun onControllerVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        setVodCtrlContainerVisibility(isVisible, anim)
    }

    protected fun setVodCtrlContainerVisibility(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            mVodCtrlContainer?.let { bottomContainer ->
                bottomContainer.visibility = VISIBLE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }
            if (showBottomProgress) {
                mTimeShiftSeekBarOverlay?.visibility = GONE
            }
        } else {
            mVodCtrlContainer?.let { bottomContainer ->
                bottomContainer.visibility = GONE
                anim?.let {
                    bottomContainer.startAnimation(it)
                }
            }

            if (showBottomProgress) {
                mTimeShiftSeekBarOverlay?.let { bottomProgress ->
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
                setProgress(0, 0)
                mTimeShiftSeekBarOverlay?.secondaryProgress = 0

            }
            UCSPlayer.STATE_PREPARED_BUT_ABORT, UCSPlayer.STATE_PREPARING,
            UCSPlayer.STATE_PREPARED, UCSPlayer.STATE_ERROR -> visibility = GONE
            UCSPlayer.STATE_PLAYING -> {
                mPlayView?.isSelected = true
                if (showBottomProgress) {
                    if (mController?.isShowing == true) {
                        mTimeShiftSeekBarOverlay?.visibility = GONE
                        mVodCtrlContainer?.visibility = VISIBLE
                    } else {
                        mVodCtrlContainer?.visibility = GONE
                        mTimeShiftSeekBarOverlay?.visibility = VISIBLE
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
        val bottomProgress = mTimeShiftSeekBarOverlay

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
        onControllerVisibilityChanged(!isLocked, null)
    }

    /**
     * 横竖屏切换
     */
    private fun toggleFullScreen() {
        mController?.toggleFullScreen()
        // 下面方法会根据适配宽高决定是否旋转屏幕
//        mControlWrapper.toggleFullScreenByVideoSize(activity);
    }

    private fun calculateSecondaryProgress(): Int {
        val percent = playerControl?.getBufferedPercentage() ?: 0
        val max = mTimeShiftBar?.max ?: 100
        return if (percent >= 95) { //解决缓冲进度不能100%问题
            max
        } else {
            (percent.toDouble() / 100 * max).toInt()
        }
    }

    override fun onProgressChanged(duration: Int, position: Int) {
        if (mTrackingTouch) {
            return
        }
        mTimeShiftBar?.let { seekBar ->
            if (duration > 0) {
                seekBar.isEnabled = true
                setProgress(position, duration)
                val secondaryProgress = calculateSecondaryProgress()
                mTimeShiftSeekBarOverlay?.secondaryProgress = secondaryProgress
            } else {
                seekBar.isEnabled = false
            }
        }
        setTotalTime(duration.toLong())
    }

    protected fun setProgress(position: Int, duration: Int) {
        var currentLabel: String? = null
        mTimeShiftBar?.let { seekBar ->
            val progress =
                if (duration == 0) 0 else (position.toDouble() / duration * seekBar.max).toInt()
            currentLabel = mTimeFormatter.format(position.toLong())
            if (seekBar is TimeShiftFloatTextIndicatorSeekBar) {
                seekBar.setProgressAndText(progress, currentLabel!!)
            } else {
                seekBar.progress = progress
            }
            seekBar.secondaryProgress = progress
            mTimeShiftSeekBarOverlay?.progress = progress
        }
        mTimeView?.text = if (currentLabel.isNullOrEmpty()) {
            mTimeFormatter.format(position.toLong())
        } else {
            currentLabel
        }
    }

    private fun setTotalTime(totalTimeMs: Long) {
        if (mTotalTimeInMs != totalTimeMs) {
            mTotalTimeInMs = totalTimeMs
            onSetDurationLabel(totalTimeMs)
        }
    }

    protected open fun onSetDurationLabel(totalTimeMs: Long) {
        mDurationView?.text = mTimeFormatter.format(totalTimeMs)
    }

    init {
        visibility = GONE
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.ucsp_ctrl_vod_control_component, this)
        }

        mFullScreenView = findViewById(R.id.ucsp_ctrl_fullScreen)
        mFullScreenView?.let {
            it.setOnClickListener(innerViewClick)
            if (isTelevisionUiMode) {//tv 模式不会显示全屏按钮
                it.visibility = View.GONE
            }
        }

        mVodCtrlContainer = findViewById(R.id.ucsp_ctrl_vodCtrlContainer)
        mTimeShiftBar = findViewById<View?>(R.id.ucsp_ctrl_seekBar) as? TimeShiftSeekBar
        mTimeShiftBar?.also {
            it.setOnSeekBarChangeListener(innerSeekBarChangeListener)
            //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
            if (Build.VERSION.SDK_INT <= 22) {
                it.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        mTimeShiftSeekBarOverlay = findViewById(R.id.ucsp_ctrl_progressOverlay)

        mDurationView = findViewById(R.id.ucsp_ctrl_duration)
        mTimeView = findViewById(R.id.ucsp_ctrl_time)
        mPlayView = findViewById(R.id.ucsp_ctrl_play)
        mPlayView?.setOnClickListener(innerViewClick)
    }
}