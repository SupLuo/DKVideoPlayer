package droid.unicstar.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import droid.unicstar.videoplayer.player.CSPlayer
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_ERROR
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_IDLE
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PAUSED
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PLAYBACK_COMPLETED
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PLAYING
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PREPARING
import droid.unicstar.videoplayer.player.CSPlayerProxy
import droid.unicstar.videoplayer.render.AspectRatioType
import droid.unicstar.videoplayer.render.Render
import droid.unicstar.videoplayer.render.Render.ScreenShotCallback
import droid.unicstar.videoplayer.render.RenderFactory
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.ProgressManager
import xyz.doikki.videoplayer.R
import xyz.doikki.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.controller.VideoViewControl
import xyz.doikki.videoplayer.internal.AudioFocusHelper
import xyz.doikki.videoplayer.internal.DKVideoViewContainer
import xyz.doikki.videoplayer.internal.ScreenModeHandler

/**
 * 播放器&播放视图  内部包含了对应的[CSPlayer] 和  [Render]，因此由本类提供这两者的功能能力
 *  本类的数据目前是在内部提供了一个容器，让容器去添加Render和Controller，这样便于界面切换
 *
 * Created by Doikki on 2017/4/7.
 *
 *
 * update by luochao on 2022/9/16
 * @see CSVideoView.playerName
 * @see CSVideoView.renderName
 * @see CSVideoView.currentState
 * @see CSVideoView.screenMode
 * @see CSVideoView.release
 * @see CSVideoView.setEnableAudioFocus
 * @see CSVideoView.setPlayerFactory
 * @see CSVideoView.setRenderViewFactory
 * @see CSVideoView.setPlayerBackgroundColor
 * @see CSVideoView.setProgressManager
 * @see CSVideoView.addOnStateChangeListener
 * @see CSVideoView.removeOnStateChangeListener
 * @see CSVideoView.clearOnStateChangeListeners
 * @see CSVideoView.setVideoController
 * @see CSVideoView.setDataSource
 * @see CSVideoView.start
 * @see CSVideoView.pause
 * @see CSVideoView.getDuration
 * @see CSVideoView.getCurrentPosition
 * @see CSVideoView.getBufferedPercentage
 * @see CSVideoView.seekTo
 * @see CSVideoView.isPlaying
 * @see CSVideoView.setVolume
 * @see CSVideoView.replay
 * @see CSVideoView.setLooping
 * @see CSVideoView.resume
 * @see CSVideoView.setSpeed
 * @see CSVideoView.getSpeed
 * @see CSVideoView.setScreenAspectRatioType
 * @see CSVideoView.screenshot
 * @see CSVideoView.setMute
 * @see CSVideoView.isMute
 * @see CSVideoView.setRotation
 * @see CSVideoView.getVideoSize
 * @see CSVideoView.getTcpSpeed
 * @see CSVideoView.setMirrorRotation
 * @see CSVideoView.isFullScreen
 * @see CSVideoView.isTinyScreen
 * @see CSVideoView.toggleFullScreen
 * @see CSVideoView.startFullScreen
 * @see CSVideoView.stopFullScreen
 * @see CSVideoView.startVideoViewFullScreen
 * @see CSVideoView.stopVideoViewFullScreen
 * @see CSVideoView.startTinyScreen
 * @see CSVideoView.stopTinyScreen

 */
open class CSVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), VideoViewControl {

    /**
     * 屏幕模式
     */
    @IntDef(
        SCREEN_MODE_NORMAL, SCREEN_MODE_FULL, SCREEN_MODE_TINY
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScreenMode

    private val mPlayerProxy: CSPlayerProxy

    /**
     * 获取渲染视图的名字
     * @return
     */
    val renderName: String get() = playerContainer.renderName

    /**
     * 当前屏幕模式：普通、全屏、小窗口
     */
    @ScreenMode
    var screenMode: Int = SCREEN_MODE_NORMAL
        private set(@ScreenMode screenMode) {
            field = screenMode
            notifyScreenModeChanged(screenMode)
        }

    /**
     * 屏幕模式切换帮助类
     */
    private val mScreenModeHandler: ScreenModeHandler = ScreenModeHandler()

    /**
     * 真正承载播放器视图的容器
     */
    @JvmField
    internal val playerContainer: DKVideoViewContainer

    private val activityContext: Activity get() = preferredActivity!!

    /**
     * 获取Activity，优先通过Controller去获取Activity
     */
    private val preferredActivity: Activity? get() = context.getActivityContext()
    protected val videoController: MediaController? get() = playerContainer.videoController


    private fun requirePlayer(): CSPlayer {
        return mPlayerProxy.requirePlayer()
    }

    /*************START 代理MediaPlayer的方法 */

    override fun setDataSource(path: String) {
        setDataSource(path, null)
    }

    override fun setDataSource(path: String, headers: Map<String, String>?) {
        mPlayerProxy.setDataSource(this.context, path, headers)
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        mPlayerProxy.setDataSource(fd)
    }

    fun prepareAsync() {
        mPlayerProxy.prepareAsync()
    }

    /*
     * release the media player in any state
     */
    private fun releasePlayer(clearTargetState: Boolean) {
        mPlayerProxy.releasePlayer(clearTargetState)
    }


    private fun attachMediaController() {
        player?.let {
            playerContainer.attachPlayer(it)
        }
    }

    /**
     * 开始播放，注意：调用此方法后必须调用[.release]释放播放器，否则会导致内存泄漏
     */
    override fun start() {
        attachMediaController()
        mPlayerProxy.start()
    }

    override fun replay(resetPosition: Boolean) {
        mPlayerProxy.replay(resetPosition)
    }

    override fun pause() {
        mPlayerProxy.pause()
    }

    /**
     * 继续播放
     */
    open fun resume() {
        mPlayerProxy.resume()
    }

    /**
     * 释放播放器
     */
    open fun release() {
        mPlayerProxy.release()
        //释放render
        playerContainer.release()
    }

    override fun getDuration(): Long {
        return mPlayerProxy.getDuration()
    }

    override fun getCurrentPosition(): Long {
        return mPlayerProxy.getCurrentPosition()
    }

    override fun getBufferedPercentage(): Int {
        return mPlayerProxy.getBufferedPercentage()
    }

    override fun seekTo(msec: Long) {
        mPlayerProxy.seekTo(msec)
    }

    override fun isPlaying(): Boolean {
        return mPlayerProxy.isPlaying()
    }

    fun setVolume(
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
    ) {
        mPlayerProxy.setVolume(leftVolume, rightVolume)
    }
    /*************END 播放器相关的代码  */


    /**--***********对外访问的方法*/

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean) {
        mPlayerProxy.setLooping(looping)
    }

    /**
     * 是否开启AudioFocus监听， 默认开启，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mPlayerProxy.setEnableAudioFocus(enableAudioFocus)
    }

    /**
     * 自定义RenderView，继承[RenderFactory]实现自己的RenderView
     */
    fun setRenderViewFactory(renderViewFactory: RenderFactory?) {
        playerContainer.setRenderViewFactory(renderViewFactory)
    }

    /**
     * 设置[.mPlayerContainer]的背景色
     */
    fun setPlayerBackgroundColor(color: Int) {
        playerContainer.setBackgroundColor(color)
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    fun setProgressManager(progressManager: ProgressManager?) {
        mPlayerProxy.setProgressManager(progressManager)
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?) {
        mediaController?.setMediaPlayer(this)
        playerContainer.setVideoController(mediaController)
        //fix：videoview先调用全屏方法后调用setController的情况下，controller的screenmode与videoview的模式不一致问题（比如引起手势无效等）
        mediaController?.setScreenMode(screenMode)
    }

    /*************START VideoViewControl  */
    override var speed: Float
        get() {
            return mPlayerProxy.getSpeed()
        }
        set(value) {
            mPlayerProxy.setSpeed(value)
        }

    override fun setScreenAspectRatioType(@AspectRatioType aspectRatioType: Int) {
        playerContainer.setScreenAspectRatioType(aspectRatioType)
    }

    override fun screenshot(highQuality: Boolean, callback: ScreenShotCallback) {
        playerContainer.screenshot(highQuality, callback)
    }

    /**
     * 设置静音
     *
     * @param isMute true:静音 false：相反
     */
    override fun setMute(isMute: Boolean) {
        mPlayerProxy.setMute(isMute)
    }

    /**
     * 是否处于静音状态
     */
    override fun isMute(): Boolean {
        return mPlayerProxy.isMute()
    }

    /**
     * 旋转视频画面
     *
     * @param degree 旋转角度
     */
    override fun setRotation(degree: Int) {
        playerContainer.setVideoRotation(degree)
    }

    /**
     * 获取视频宽高,其中width: mVideoSize[0], height: mVideoSize[1]
     */
    override fun getVideoSize(): IntArray {
        //是否适合直接返回该变量,存在被外层修改的可能？是否应该 return new int[]{mVideoSize[0], mVideoSize[1]}
        return playerContainer.videoSize
    }

    /**
     * 获取缓冲速度
     */
    override fun getTcpSpeed(): Long {
        return mPlayerProxy.getTcpSpeed()
    }

    /**
     * 设置镜像旋转，暂不支持SurfaceView
     */
    override fun setMirrorRotation(enable: Boolean) {
        playerContainer.setVideoMirrorRotation(enable)
    }

    /**
     * 判断是否处于全屏状态（视图处于全屏）
     */
    override fun isFullScreen(): Boolean {
        return screenMode == SCREEN_MODE_FULL
    }

    /**
     * 当前是否处于小屏状态（视图处于小屏）
     */
    override fun isTinyScreen(): Boolean {
        return screenMode == SCREEN_MODE_TINY
    }

    /**
     * 横竖屏切换
     *
     * @return
     */
    override fun toggleFullScreen(): Boolean {
        return if (isFullScreen) {
            stopFullScreen()
        } else {
            startFullScreen()
        }
    }

    /**
     * 开始全屏
     */
    override fun startFullScreen(isLandscapeReversed: Boolean): Boolean {
        //设置界面横屏
        preferredActivity?.let { activity ->
            if (isLandscapeReversed) {
                if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            } else {
                if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }
        return startVideoViewFullScreen()
    }

    /**
     * 停止全屏
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun stopFullScreen(): Boolean {
        preferredActivity?.let { activity ->
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        return stopVideoViewFullScreen()
    }

    /**
     * VideoView全屏
     */
    override fun startVideoViewFullScreen(): Boolean {
        if (isFullScreen) return false
        if (mScreenModeHandler.startFullScreen(activityContext, playerContainer)) {
            screenMode = SCREEN_MODE_FULL
            return true
        }
        return false
    }

    /**
     * VideoView退出全屏
     */
    override fun stopVideoViewFullScreen(): Boolean {
        if (!isFullScreen) return false
        if (mScreenModeHandler.stopFullScreen(activityContext, this, playerContainer)) {
            screenMode = SCREEN_MODE_NORMAL
            return true
        }
        return false
    }

    /**
     * 开启小屏
     */
    override fun startTinyScreen() {
        if (isTinyScreen) return
        if (mScreenModeHandler.startTinyScreen(activityContext, playerContainer)) {
            screenMode = SCREEN_MODE_TINY
        }
    }

    /**
     * 退出小屏
     */
    override fun stopTinyScreen() {
        if (!isTinyScreen) return
        if (mScreenModeHandler.stopTinyScreen(this, playerContainer)) {
            screenMode = SCREEN_MODE_NORMAL
        }
    }

    /*************START VideoViewControl  */


    /**
     * 通知当前界面模式发生了变化
     */
    @CallSuper
    protected fun notifyScreenModeChanged(@ScreenMode screenMode: Int) {
        //todo 既然通过通知对外发布了screenmode的改变，是否就不应该再主动
        videoController?.setScreenMode(screenMode)
        mStateChangedListeners.forEach {
            it.onScreenModeChanged(screenMode)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isFullScreen) {
            //重新获得焦点时保持全屏状态
            ScreenModeHandler.hideSystemBar(activityContext)
        }
    }

    /**
     * 播放状态改变监听器
     * todo 目前VideoView对外可访问的回调过少，[CSPlayer.EventListener]的回调太多对外不可见
     */
    interface OnStateChangeListener {

        fun onScreenModeChanged(@ScreenMode screenMode: Int) {}

        /**
         * 播放器播放状态发生了变化
         *
         * @param playState
         */
        fun onPlayerStateChanged(@CSPlayer.PlayState playState: Int) {}

    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return playerContainer.onBackPressed()
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

    private fun onVideoSizeChanged(width:Int,height:Int){
        this@CSVideoView.playerContainer.onVideoSizeChanged(width, height)
    }

    private val mStateChangeListener = object : CSPlayer.OnPlayStateChangeListener {
        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            when (playState) {
                STATE_PLAYING -> {
                    if (!this@CSVideoView.keepScreenOn)
                        this@CSVideoView.keepScreenOn = true
                }
                STATE_PAUSED, STATE_PLAYBACK_COMPLETED, STATE_ERROR -> {
                    if (this@CSVideoView.keepScreenOn)
                        this@CSVideoView.keepScreenOn = false
                }
                STATE_PREPARING ->{
                    attachMediaController()
                }
            }
            videoController?.setPlayerState(playState)
        }
    }

    private val mEventListener = object : CSPlayer.EventListener {
        override fun onInfo(what: Int, extra: Int) {
            super.onInfo(what, extra)
            when(what){
                CSPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> setRotation(extra)
            }
        }
        /**
         * 播放信息回调，播放中的缓冲开始与结束，开始渲染视频第一帧，视频旋转信息
         */

        override fun onVideoSizeChanged(width: Int, height: Int) {
            this@CSVideoView.onVideoSizeChanged(width, height)
        }
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
        playerContainer = DKVideoViewContainer(context).also {
            it.setBackgroundColor(playerBackgroundColor)
        }
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(playerContainer, params)
        playerContainer.setScreenAspectRatioType(screenAspectRatioType)

        mPlayerProxy = CSPlayerProxy(this.context)
        mPlayerProxy.setEventListener(mEventListener)
        mPlayerProxy.addOnPlayStateChangeListener(mStateChangeListener)
    }
}