package droid.unicstar.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import droid.unicstar.videoplayer.controller.UNSRenderControl
import droid.unicstar.videoplayer.controller.UNSWindowModelControl
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.player.UNSPlayer.Companion.STATE_ERROR
import droid.unicstar.videoplayer.player.UNSPlayer.Companion.STATE_PAUSED
import droid.unicstar.videoplayer.player.UNSPlayer.Companion.STATE_PLAYBACK_COMPLETED
import droid.unicstar.videoplayer.player.UNSPlayer.Companion.STATE_PLAYING
import droid.unicstar.videoplayer.player.UNSPlayer.Companion.STATE_PREPARING
import droid.unicstar.videoplayer.player.UNSPlayerProxy
import droid.unicstar.videoplayer.render.AspectRatioType
import droid.unicstar.videoplayer.render.UNSRender
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.ProgressManager
import xyz.doikki.videoplayer.R
import xyz.doikki.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.controller.VideoViewControl
import xyz.doikki.videoplayer.internal.AudioFocusHelper
import xyz.doikki.videoplayer.internal.ScreenModeHandler

/**
 * 播放器&播放视图  内部包含了对应的[UNSPlayer]和 [UNSRender]，因此由本类提供这两者的功能能力
 *  本类的数据目前是在内部提供了一个容器，让容器去添加Render和Controller，这样便于界面切换
 *
 * Created by Doikki on 2017/4/7.
 *
 *
 * update by luochao on 2022/9/16
 * @see UNSVideoView.playerName
 * @see UNSVideoView.renderName
 * @see UNSVideoView.currentState
 * @see UNSVideoView.screenMode
 * @see UNSVideoView.release
 * @see UNSVideoView.setEnableAudioFocus
 * @see UNSVideoView.setPlayerFactory
 * @see UNSVideoView.setRenderViewFactory
 * @see UNSVideoView.setPlayerBackgroundColor
 * @see UNSVideoView.setProgressManager
 * @see UNSVideoView.addOnStateChangeListener
 * @see UNSVideoView.removeOnStateChangeListener
 * @see UNSVideoView.clearOnStateChangeListeners
 * @see UNSVideoView.setVideoController
 * @see UNSVideoView.setDataSource
 * @see UNSVideoView.start
 * @see UNSVideoView.pause
 * @see UNSVideoView.getDuration
 * @see UNSVideoView.getCurrentPosition
 * @see UNSVideoView.getBufferedPercentage
 * @see UNSVideoView.seekTo
 * @see UNSVideoView.isPlaying
 * @see UNSVideoView.setVolume
 * @see UNSVideoView.replay
 * @see UNSVideoView.setLooping
 * @see UNSVideoView.resume
 * @see UNSVideoView.speed
 * @see UNSVideoView.setScreenAspectRatioType
 * @see UNSVideoView.screenshot
 * @see UNSVideoView.setMute
 * @see UNSVideoView.isMute
 * @see UNSVideoView.setRotation
 * @see UNSVideoView.getVideoSize
 * @see UNSVideoView.getTcpSpeed
 * @see UNSVideoView.setMirrorRotation
 * @see UNSVideoView.isFullScreen
 * @see UNSVideoView.isTinyScreen
 * @see UNSVideoView.toggleFullScreen
 * @see UNSVideoView.startFullScreen
 * @see UNSVideoView.stopFullScreen
 * @see UNSVideoView.startVideoViewFullScreen
 * @see UNSVideoView.stopVideoViewFullScreen
 * @see UNSVideoView.startTinyScreen
 * @see UNSVideoView.stopTinyScreen

 */
open class UNSVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val mDisplayContainer: UNSDisplayContainer
) : FrameLayout(context, attrs), VideoViewControl, UNSRenderControl by mDisplayContainer,
    UNSWindowModelControl by mDisplayContainer {

    /**
     * 屏幕状态发生变化容器
     */
    fun interface OnScreenModeChangeListener {
        fun onScreenModeChanged(@ScreenMode screenMode: Int)
    }

    /**
     * 屏幕模式
     */
    @IntDef(SCREEN_MODE_NORMAL, SCREEN_MODE_FULL, SCREEN_MODE_TINY)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScreenMode

    //播放器
    private val mPlayer: UNSPlayerProxy

    private val mActivity: Activity get() = mPreferredActivity!!

    //获取Activity，优先通过Controller去获取Activity
    private val mPreferredActivity: Activity? get() = context.getActivityContext()

    /**
     * 获取播放器名字
     */
    val playerName: String get() = mPlayer.playerName

    /**
     * 获取渲染视图的名字
     */
    val renderName: String get() = mDisplayContainer.renderName

    protected val videoController: MediaController? get() = mDisplayContainer.videoController

    private val mStateChangeListener = UNSPlayer.OnPlayStateChangeListener { playState ->
        when (playState) {
            STATE_PLAYING -> {
                if (!this@UNSVideoView.keepScreenOn)
                    this@UNSVideoView.keepScreenOn = true
            }
            STATE_PAUSED, STATE_PLAYBACK_COMPLETED, STATE_ERROR -> {
                if (this@UNSVideoView.keepScreenOn)
                    this@UNSVideoView.keepScreenOn = false
            }
            STATE_PREPARING -> {
                attachMediaController()
            }
        }
        videoController?.setPlayerState(playState)
    }

    private val mEventListener = object : UNSPlayer.EventListener {
        override fun onInfo(what: Int, extra: Int) {
            super.onInfo(what, extra)
            when (what) {
                UNSPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED ->
                    mDisplayContainer.setVideoRotation(extra)
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            mDisplayContainer.setVideoSize(width, height)
        }
    }

    /**
     * 添加屏幕变化监听
     */
    fun addOnScreenModeChangeListener(listener: OnScreenModeChangeListener) {
        mDisplayContainer.addOnScreenModeChangeListener(listener)
    }

    /**
     * 移除屏幕模式变化监听
     */
    fun removeOnScreenModeChangeListener(listener: OnScreenModeChangeListener) {
        mDisplayContainer.removeOnScreenModeChangeListener(listener)
    }

    /*************START 代理MediaPlayer的方法 */

    fun setDataSource(path: String) {
        mPlayer.setDataSource(context, path)
    }

    fun setDataSource(path: String, headers: Map<String, String>?) {
        mPlayer.setDataSource(context, path, headers)
    }

    fun setDataSource(fd: AssetFileDescriptor) {
        mPlayer.setDataSource(fd)
    }

    fun prepareAsync() {
        mPlayer.prepareAsync()
    }

    private fun attachMediaController() {
        player?.let {
            mDisplayContainer.attachPlayer(it)
        }
    }

    /**
     * 开始播放，注意：调用此方法后必须调用[.release]释放播放器，否则会导致内存泄漏
     */
    override fun start() {
        attachMediaController()
        mPlayer.start()
    }

    override fun replay(resetPosition: Boolean) {
        mPlayer.replay(resetPosition)
    }

    override fun pause() {
        mPlayer.pause()
    }

    /**
     * 继续播放
     */
    open fun resume() {
        mPlayer.resume()
    }

    /**
     * 释放播放器
     */
    open fun release() {
        mPlayer.release()
        //释放render
        mDisplayContainer.release()
    }

    override fun getDuration(): Long {
        return mPlayer.getDuration()
    }

    override fun getCurrentPosition(): Long {
        return mPlayer.getCurrentPosition()
    }

    override fun getBufferedPercentage(): Int {
        return mPlayer.getBufferedPercentage()
    }

    override fun seekTo(msec: Long) {
        mPlayer.seekTo(msec)
    }

    override fun isPlaying(): Boolean {
        return mPlayer.isPlaying()
    }

    fun setVolume(
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
    ) {
        mPlayer.setVolume(leftVolume, rightVolume)
    }
    /*************END 播放器相关的代码  */


    /**--***********对外访问的方法*/

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean) {
        mPlayer.setLooping(looping)
    }

    /**
     * 是否开启AudioFocus监听， 默认开启，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mPlayer.setEnableAudioFocus(enableAudioFocus)
    }

    /**
     * 设置[.mPlayerContainer]的背景色
     */
    fun setPlayerBackgroundColor(color: Int) {
        mDisplayContainer.setBackgroundColor(color)
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    fun setProgressManager(progressManager: ProgressManager?) {
        mPlayer.setProgressManager(progressManager)
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?) {
        mediaController?.setMediaPlayer(this)
        mDisplayContainer.setVideoController(mediaController)
    }

    /*************START VideoViewControl  */
    override var speed: Float
        get() {
            return mPlayer.getSpeed()
        }
        set(value) {
            mPlayer.setSpeed(value)
        }

    /**
     * 设置静音
     * @param isMute true:静音 false：相反
     */
    override fun setMute(isMute: Boolean) {
        mPlayer.setMute(isMute)
    }

    /**
     * 是否处于静音状态
     */
    override fun isMute(): Boolean {
        return mPlayer.isMute()
    }

    /**
     * 获取缓冲速度
     */
    override fun getTcpSpeed(): Long {
        return mPlayer.getTcpSpeed()
    }


    /*************START WindowModeControl  */
    /**
     * 判断是否处于全屏状态（视图处于全屏）
     */
    fun isFullScreen(): Boolean {
        return mDisplayContainer.isFullScreen()
    }

    /**
     * 当前是否处于小屏状态（视图处于小屏）
     */
    fun isTinyScreen(): Boolean {
        return mDisplayContainer.isTinyScreen()
    }

    /**
     * 横竖屏切换
     * @return
     * @note 由于设计上支持界面间共享播放器，因此需要明确指定[Activity]对象
     */
    fun toggleFullScreen(activity: Activity = mActivity): Boolean {
        return mDisplayContainer.toggleFullScreen(activity, this)
    }

    /**
     * 开始全屏
     */
    @JvmOverloads
    fun startFullScreen(
        activity: Activity = mActivity,
        isLandscapeReversed: Boolean = false
    ): Boolean {
        return mDisplayContainer.startFullScreen(activity, isLandscapeReversed)
    }

    /**
     * 整个播放视图（Render、Controller）全屏
     * @param activity 在指定界面全屏
     */
    @JvmOverloads
    fun startVideoViewFullScreen(
        activity: Activity = mActivity,
        tryHideSystemBar: Boolean = true
    ): Boolean {
        return mDisplayContainer.startVideoViewFullScreen(activity, tryHideSystemBar)
    }

    /**
     * 停止全屏
     */
    @SuppressLint("SourceLockedOrientationActivity")
    @JvmOverloads
    fun stopFullScreen(activity: Activity? = mPreferredActivity): Boolean {
        return mDisplayContainer.stopFullScreen(this, activity)
    }

    /**
     * 整个播放视图（Render、Controller）退出全屏
     */
    @JvmOverloads
    fun stopVideoViewFullScreen(tryShowSystemBar: Boolean = true): Boolean {
        return mDisplayContainer.stopVideoViewFullScreen(this, tryShowSystemBar)
    }

    /**
     * 开启小屏
     */
    @JvmOverloads
    fun startTinyScreen(activity: Activity = mActivity) {
        mDisplayContainer.startTinyScreen(activity)
    }

    /**
     * 退出小屏
     */
    fun stopTinyScreen() {
        mDisplayContainer.stopTinyScreen(this)
    }

    /*************END WindowModeControl  */

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isFullScreen()) {
            //todo tv/盒子开发不要处理
            //重新获得焦点时保持全屏状态
            ScreenModeHandler.hideSystemBar(mActivity)
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return mDisplayContainer.onBackPressed()
    }

//    override fun onSaveInstanceState(): Parcelable? {
//        L.d("onSaveInstanceState: currentPosition=$mSeekWhenPrepared")
//        //activity切到后台后可能被系统回收，故在此处进行进度保存
//        saveCurrentPlayedProgress()
//        return super.onSaveInstanceState()
//    }//读取播放进度


    companion object {
        /**
         * 屏幕比例类型
         */
        const val SCREEN_ASPECT_RATIO_DEFAULT = AspectRatioType.DEFAULT_SCALE
        const val SCREEN_ASPECT_RATIO_SCALE_18_9 = AspectRatioType.SCALE_18_9
        const val SCREEN_ASPECT_RATIO_SCALE_16_9 = AspectRatioType.SCALE_16_9
        const val SCREEN_ASPECT_RATIO_SCALE_4_3 = AspectRatioType.SCALE_4_3
        const val SCREEN_ASPECT_RATIO_MATCH_PARENT = AspectRatioType.MATCH_PARENT
        const val SCREEN_ASPECT_RATIO_SCALE_ORIGINAL = AspectRatioType.SCALE_ORIGINAL
        const val SCREEN_ASPECT_RATIO_CENTER_CROP = AspectRatioType.CENTER_CROP

        /**
         * 普通模式
         */
        const val SCREEN_MODE_NORMAL = 10

        /**
         * 全屏模式
         */
        const val SCREEN_MODE_FULL = 11

        /**
         * 小窗模式
         */
        const val SCREEN_MODE_TINY = 22
    }


    init {

        //读取xml中的配置，并综合全局配置
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DKVideoView)

        mAudioFocusHelper.isEnable =
            ta.getBoolean(R.styleable.DKVideoView_enableAudioFocus, DKManager.isAudioFocusEnabled)
        mLooping = ta.getBoolean(R.styleable.DKVideoView_looping, false)

        val screenAspectRatioType =
            ta.getInt(R.styleable.DKVideoView_screenScaleType, DKManager.screenAspectRatioType)
        val playerBackgroundColor =
            ta.getColor(R.styleable.DKVideoView_playerBackgroundColor, Color.BLACK)
        ta.recycle()

        //准备播放器容器
        mDisplayContainer = UNSDisplayContainer(context).also {
            it.setBackgroundColor(playerBackgroundColor)
        }
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(mDisplayContainer, params)


        mPlayer = UNSPlayerProxy(this.context)
        mPlayer.setEventListener(mEventListener)
        mPlayer.addOnPlayStateChangeListener(mStateChangeListener)
    }
}