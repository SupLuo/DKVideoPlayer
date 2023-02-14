package xyz.doikki.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.net.Uri
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import xyz.doikki.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.controller.VideoViewControl
import xyz.doikki.videoplayer.internal.AudioFocusHelper
import xyz.doikki.videoplayer.internal.DKVideoViewContainer
import xyz.doikki.videoplayer.internal.ScreenModeHandler
import xyz.doikki.videoplayer.render.AspectRatioType
import xyz.doikki.videoplayer.render.Render
import xyz.doikki.videoplayer.render.Render.ScreenShotCallback
import xyz.doikki.videoplayer.render.RenderFactory
import xyz.doikki.videoplayer.util.L
import xyz.doikki.videoplayer.util.getActivityContext
import xyz.doikki.videoplayer.util.orDefault
import xyz.doikki.videoplayer.util.tryIgnore
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播放器&播放视图  内部包含了对应的[DKPlayer] 和  [Render]，因此由本类提供这两者的功能能力
 *  本类的数据目前是在内部提供了一个容器，让容器去添加Render和Controller，这样便于界面切换
 *
 * Created by Doikki on 2017/4/7.
 *
 *
 * update by luochao on 2022/9/16
 * @see DKVideoView.playerName
 * @see DKVideoView.renderName
 * @see DKVideoView.currentState
 * @see DKVideoView.screenMode
 * @see DKVideoView.release
 * @see DKVideoView.setEnableAudioFocus
 * @see DKVideoView.setPlayerFactory
 * @see DKVideoView.setRenderViewFactory
 * @see DKVideoView.setPlayerBackgroundColor
 * @see DKVideoView.setProgressManager
 * @see DKVideoView.addOnStateChangeListener
 * @see DKVideoView.removeOnStateChangeListener
 * @see DKVideoView.clearOnStateChangeListeners
 * @see DKVideoView.setVideoController
 * @see DKVideoView.setDataSource
 * @see DKVideoView.start
 * @see DKVideoView.pause
 * @see DKVideoView.getDuration
 * @see DKVideoView.getCurrentPosition
 * @see DKVideoView.getBufferedPercentage
 * @see DKVideoView.seekTo
 * @see DKVideoView.isPlaying
 * @see DKVideoView.setVolume
 * @see DKVideoView.replay
 * @see DKVideoView.setLooping
 * @see DKVideoView.resume
 * @see DKVideoView.setSpeed
 * @see DKVideoView.getSpeed
 * @see DKVideoView.setScreenAspectRatioType
 * @see DKVideoView.screenshot
 * @see DKVideoView.setMute
 * @see DKVideoView.isMute
 * @see DKVideoView.setRotation
 * @see DKVideoView.getVideoSize
 * @see DKVideoView.getTcpSpeed
 * @see DKVideoView.setMirrorRotation
 * @see DKVideoView.isFullScreen
 * @see DKVideoView.isTinyScreen
 * @see DKVideoView.toggleFullScreen
 * @see DKVideoView.startFullScreen
 * @see DKVideoView.stopFullScreen
 * @see DKVideoView.startVideoViewFullScreen
 * @see DKVideoView.stopVideoViewFullScreen
 * @see DKVideoView.startTinyScreen
 * @see DKVideoView.stopTinyScreen

 */
open class DKVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), VideoViewControl, DKPlayer.EventListener {

    /**
     * 播放器状态
     */
    @IntDef(
        //出错
        STATE_ERROR,
        //闲置
        STATE_IDLE,
        //准备数据源中：setDatasource与onPrepared之间
        STATE_PREPARING,
        //数据源已准备：onPrepared回调
        STATE_PREPARED,
        //开始播放：调用start()之后
        STATE_PLAYING,
        //暂停
        STATE_PAUSED,
        //播放结束
        STATE_PLAYBACK_COMPLETED,
        //缓冲中
        STATE_BUFFERING,
        //缓冲结束
        STATE_BUFFERED,
        //已准备但因为用户设置不允许移动网络播放而中断：onPrepared回调之后并没有调用start
        STATE_PREPARED_BUT_ABORT
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PlayState

    /**
     * 屏幕模式
     */
    @IntDef(
        SCREEN_MODE_NORMAL, SCREEN_MODE_FULL, SCREEN_MODE_TINY
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScreenMode

    /**
     * 播放器内核
     */
    protected var player: DKPlayer? = null
        private set

    /**
     * 获取播放器名字
     * @return
     */
    val playerName: String
        get() {
            val className = mPlayerFactory.orDefault(DKManager.playerFactory).javaClass.name
            return className.substring(className.lastIndexOf(".") + 1)
        }

    /**
     * 获取渲染视图的名字
     * @return
     */
    val renderName: String get() = playerContainer.renderName

    // recording the seek position while preparing
    private var mSeekWhenPrepared: Long = 0

    //当前播放位置：用于重新播放（或重试时）恢复之前的播放位置
    private var mCurrentPosition: Long = 0

    /**
     * 当前播放器的状态
     * mCurrentState is a VideoView object's current state.
     * mTargetState is the state that a method caller intends to reach.
     * For instance, regardless the VideoView object's current state,
     * calling pause() intends to bring the object to a target state
     * of STATE_PAUSED.
     */
    @PlayState
    var currentState: Int = STATE_IDLE
        protected set(@PlayState state) {
            if (field != state) {
                field = state
                notifyPlayerStateChanged()
            }
        }
    private var mTargetState: Int = STATE_IDLE

    /**
     * 当前屏幕模式：普通、全屏、小窗口
     */
    @DKVideoView.ScreenMode
    var screenMode: Int = SCREEN_MODE_NORMAL
        private set(@DKVideoView.ScreenMode screenMode) {
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

    /**
     * 自定义播放器构建工厂
     */
    private var mPlayerFactory: DKPlayerFactory<out DKPlayer>? = null

    /**
     * 是否静音
     */
    private var mMute = false

    /**
     * 左声道音量
     */
    private var mLeftVolume = 1.0f

    /**
     * 右声道音量
     */
    private var mRightVolume = 1.0f

    /**
     * 是否循环播放
     */
    private var mLooping = false

    /**
     * 音频焦点管理帮助类
     */
    private val mAudioFocusHelper: AudioFocusHelper = AudioFocusHelper(this)

    /**
     * OnStateChangeListener集合，保存了所有开发者设置的监听器
     */
    private val mStateChangedListeners = CopyOnWriteArrayList<OnStateChangeListener>()

    //--------- data sources ---------//
    /**
     * 当前播放视频的地址
     */
    private var mUrl: String? = null

    /**
     * 当前视频地址的请求头
     */
    private var mHeaders: Map<String, String>? = null

    /**
     * 用于播放assets里面的视频文件
     */
    private var mAssetFileDescriptor: AssetFileDescriptor? = null

    //--------- end data sources ---------//

    /**
     * 进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
     */
    protected var progressManager: ProgressManager? = DKManager.progressManager
        private set

    private val activityContext: Activity get() = preferredActivity!!

    /**
     * 获取Activity，优先通过Controller去获取Activity
     */
    private val preferredActivity: Activity? get() = context.getActivityContext()

    /**
     * 判断是否为本地数据源，包括 本地文件、Asset、raw
     */
    private val isLocalDataSource: Boolean
        get() {
            if (mAssetFileDescriptor != null) {
                return true
            }
            if (!mUrl.isNullOrEmpty()) {
                val uri = Uri.parse(mUrl)
                return ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme || ContentResolver.SCHEME_FILE == uri.scheme || "rawresource" == uri.scheme
            }
            return false
        }

    protected val videoController: MediaController? get() = playerContainer.videoController

    /**
     * 是否显示移动网络提示，可在Controller中配置
     * 非本地数据源并且控制器需要显示网络提示
     */
    private val showNetworkWarning: Boolean
        get() = !isLocalDataSource && videoController?.showNetWarning().orDefault()

    private fun requirePlayer(): DKPlayer {
        return player ?: throw IllegalStateException("请先创建播放器（prepareMediaPlayer）")
    }

    /*************START 代理MediaPlayer的方法 */

    override fun setDataSource(path: String) {
        setDataSource(path, null)
    }

    override fun setDataSource(path: String, headers: Map<String, String>?) {
        mAssetFileDescriptor = null
        mUrl = path
        mHeaders = headers
        mSeekWhenPrepared = getSavedPlayedProgress(path)
        prepareAsync()
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        mUrl = null
        mAssetFileDescriptor = fd
        mSeekWhenPrepared = 0
        prepareAsync()
    }

    /**
     * 开始准备：缓冲数据源
     */
    private fun prepareAsync() {
        try {
            val asset = mAssetFileDescriptor
            val url = mUrl
            // not ready for playback just yet, will try again later
            require(asset != null || !url.isNullOrEmpty()) {
                "data source is null,please set first."
            }
            //确保播放器内核
            ensurePlayer()
            prepareKernelDataSource()
        } catch (e: Throwable) {
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mStateChangedListeners.forEach {
                it.onPlayerError(e)
            }
        }
    }

    protected open fun prepareKernelDataSource() {
        val asset = mAssetFileDescriptor
        val url = mUrl
        // not ready for playback just yet, will try again later
        if (asset == null && url.isNullOrEmpty())
            return
        val player = requirePlayer()
        if (asset != null) {
            player.setDataSource(asset)
        } else if (!url.isNullOrEmpty()) {
            player.setDataSource(url, mHeaders)
        }
        player.prepareAsync()
        currentState = STATE_PREPARING
    }

    private fun openVideo() {
        try {
            val asset = mAssetFileDescriptor
            val url = mUrl
            // not ready for playback just yet, will try again later
            if (asset == null && url.isNullOrEmpty())
                return
            releasePlayer(false)
            ensurePlayer()
            attachMediaController()
            prepareKernelDataSource()
        } catch (e: Throwable) {
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mStateChangedListeners.forEach {
                it.onPlayerError(e)
            }
        }
    }

    /*
     * release the media player in any state
     */
    private fun releasePlayer(clearTargetState: Boolean) {
        player?.let {
            it.reset()
            it.release()
            player = null
            currentState = STATE_IDLE
            if (clearTargetState) {
                mTargetState = STATE_IDLE
            }
            mAudioFocusHelper.abandonFocus()
        }
    }

    /**
     * 确保播放器可用
     */
    protected open fun ensurePlayer() {
        player = createPlayer().also {
            it.setEventListener(this)
            it.init()
        }
        preparePlayerOptions()
    }

    /**
     * 创建播放器
     */
    protected open fun createPlayer(): DKPlayer {
        return DKManager.createMediaPlayer(context, mPlayerFactory).also {
            Log.d(
                "DKPlayer", "使用播放器${
                    it.javaClass.name.run {
                        this.substring(this.lastIndexOf("."))
                    }
                }"
            )
        }
    }

    /**
     * 准备播放器的配置项
     */
    protected open fun preparePlayerOptions() {
        setLooping(mLooping)
        isMute = mMute
    }

    private fun attachMediaController() {
        player?.let {
            playerContainer.attachPlayer(it)
        }
    }

    /**
     * 是否处于可播放状态
     */
    fun isInPlaybackState(): Boolean {
        return player != null
                && currentState != STATE_IDLE
                && currentState != STATE_ERROR
                && currentState != STATE_PLAYBACK_COMPLETED
                && currentState != STATE_PREPARING
    }

    /**
     * 开始播放，注意：调用此方法后必须调用[.release]释放播放器，否则会导致内存泄漏
     */
    override fun start() {
        attachMediaController()

        //已就绪，准备开始播放
        if (isInPlaybackState()) {
            //移动网络不允许播放
            if (currentState == STATE_PREPARED_BUT_ABORT) {
                return
            }
            //进行移动流量播放提醒
            if (showNetworkWarning && currentState != STATE_PREPARED_BUT_ABORT) {
                //中止播放
                currentState = STATE_PREPARED_BUT_ABORT
                return
            }
            startInPlaybackState()
        } else {
//            if (currentState == STATE_IDLE || player == null) {
//                openVideo()
//            }
            mTargetState = STATE_PLAYING
        }
    }

    override fun replay(resetPosition: Boolean) {
        //用于恢复之前播放的位置
        if (!resetPosition && mCurrentPosition > 0) {
            mSeekWhenPrepared = mCurrentPosition
        }
        player?.reset()
        //重新设置option，media player reset之后，option会失效
        preparePlayerOptions()
        attachMediaController()
        prepareKernelDataSource()
        start()
    }

    /**
     * 播放状态下开始播放
     */
    protected open fun startInPlaybackState() {
        requirePlayer().start()
        if (!isMute) {
            mAudioFocusHelper.requestFocus()
        }
        currentState = STATE_PLAYING
        playerContainer.keepScreenOn = true
    }

    /**
     * 获取已保存的当前播放进度
     *
     * @return
     */
    private fun getSavedPlayedProgress(url: String): Long {
        return progressManager?.getSavedProgress(url).orDefault()
    }

    override fun pause() {
        player?.let { player ->
            if (isInPlaybackState() && player.isPlaying()) {
                player.pause()
                currentState = STATE_PAUSED
                if (!isMute) {
                    mAudioFocusHelper.abandonFocus()
                }
                playerContainer.keepScreenOn = false
            }
        }
        mTargetState = STATE_PAUSED
    }

    /**
     * 继续播放
     */
    open fun resume() {
        player?.let { player ->
            if (isInPlaybackState() && !player.isPlaying()) {
                player.start()
                currentState = STATE_PLAYING
                if (!isMute) {
                    mAudioFocusHelper.requestFocus()
                }
                playerContainer.keepScreenOn = true
            }
        }
        mTargetState = STATE_PLAYING
    }

    open fun stopPlayback() {
        //释放播放器
        player?.release()
        player = null
        //释放render
        playerContainer.release()
        //释放Assets资源
        mAssetFileDescriptor?.let {
            tryIgnore {
                it.close()
            }
        }
        //关闭AudioFocus监听
        mAudioFocusHelper.abandonFocus()
        //保存播放进度
        saveCurrentPlayedProgress()
        //重置播放进度
        mSeekWhenPrepared = 0
        mCurrentPosition = 0
        //切换转态
        currentState = STATE_IDLE
        mTargetState = STATE_IDLE
    }

    /**
     * 释放播放器
     */
    open fun release() {
        if (currentState != STATE_IDLE) {
            //释放播放器
            player?.release()
            player = null
            //释放render
            playerContainer.release()
            //释放Assets资源
            mAssetFileDescriptor?.let {
                tryIgnore {
                    it.close()
                }
            }
            //关闭AudioFocus监听
            mAudioFocusHelper.abandonFocus()
            //保存播放进度
            saveCurrentPlayedProgress()
            //重置播放进度
            mSeekWhenPrepared = 0
            //切换转态
            currentState = STATE_IDLE
            mTargetState = STATE_IDLE
        }
    }

    override fun getDuration(): Long {
        return if (isInPlaybackState()) {
            player?.getDuration().orDefault()
        } else -1
    }

    override fun getCurrentPosition(): Long {
        if (isInPlaybackState()) {
            mCurrentPosition = requirePlayer().getCurrentPosition()
            return mCurrentPosition
        }
        return 0
    }

    override fun getBufferedPercentage(): Int {
        return player?.getBufferedPercentage().orDefault()
    }

    override fun seekTo(msec: Long) {
        mSeekWhenPrepared = if (isInPlaybackState()) {
            player?.seekTo(msec)
            0
        } else {
            msec
        }
    }

    override fun isPlaying(): Boolean {
        return isInPlaybackState() && player?.isPlaying().orDefault()
    }

    fun setVolume(
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
    ) {
        mLeftVolume = leftVolume
        mRightVolume = rightVolume
        player?.setVolume(leftVolume, rightVolume)
    }
    /*************END 播放器相关的代码  */


    /**--***********对外访问的方法*/

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean) {
        mLooping = looping
        player?.setLooping(looping)
    }

    /**
     * 是否开启AudioFocus监听， 默认开启，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mAudioFocusHelper.isEnable = enableAudioFocus
    }

    /**
     * 自定义播放核心，继承[DKPlayerFactory]实现自己的播放核心
     */
    fun setPlayerFactory(playerFactory: DKPlayerFactory<out DKPlayer>) {
        mPlayerFactory = playerFactory
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
        this.progressManager = progressManager
    }

    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        mStateChangedListeners.add(listener)
    }

    /**
     * 移除某个播放状态监听
     */
    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        mStateChangedListeners.remove(listener)
    }

    /**
     * 移除所有播放状态监听
     */
    fun clearOnStateChangeListeners() {
        mStateChangedListeners.clear()
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
            return if (isInPlaybackState()) {
                player?.getSpeed().orDefault(1f)
            } else 1f
        }
        set(value) {
            if (isInPlaybackState()) {
                player?.setSpeed(value)
            }
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
        mMute = isMute
        player?.let { player ->
            val leftVolume = if (isMute) 0.0f else mLeftVolume
            val rightVolume = if (isMute) 0.0f else mRightVolume
            player.setVolume(leftVolume, rightVolume)
        }
    }

    /**
     * 是否处于静音状态
     */
    override fun isMute(): Boolean {
        return mMute
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
        return player?.getTcpSpeed().orDefault()
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

    /*************START AVPlayer#EventListener 实现逻辑 */
    /**
     * 视频缓冲完毕，准备开始播放时回调
     */
    override fun onPrepared() {
        currentState = STATE_PREPARED
        if (mSeekWhenPrepared > 0) {
            seekTo(mSeekWhenPrepared)
        }
        if (mTargetState == STATE_PLAYING) {
            start()
        }
    }

    /**
     * 播放信息回调，播放中的缓冲开始与结束，开始渲染视频第一帧，视频旋转信息
     */
    override fun onInfo(what: Int, extra: Int) {
        when (what) {
            DKPlayer.MEDIA_INFO_BUFFERING_START -> currentState = STATE_BUFFERING
            DKPlayer.MEDIA_INFO_BUFFERING_END -> currentState = STATE_BUFFERED
            DKPlayer.MEDIA_INFO_RENDERING_START -> {
                currentState = STATE_PLAYING
                playerContainer.keepScreenOn = true
            }
            DKPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> setRotation(extra)
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        playerContainer.onVideoSizeChanged(width, height)
    }

    /**
     * 视频播放出错回调
     */
    override fun onError(e: Throwable) {
        playerContainer.keepScreenOn = false
        currentState = STATE_ERROR
        mTargetState = STATE_ERROR
        mStateChangedListeners.forEach {
            it.onPlayerError(e)
        }
    }

    /**
     * 视频播放完成回调
     */
    override fun onCompletion() {
        playerContainer.keepScreenOn = false
        mSeekWhenPrepared = 0
        mCurrentPosition = 0
        //播放完成，清除进度
        savePlayedProgress(mUrl, 0)
        currentState = STATE_PLAYBACK_COMPLETED
        mTargetState = STATE_PLAYBACK_COMPLETED
    }
    /*************END AVPlayer#EventListener 实现逻辑 */

    /**
     * 通知播放器状态发生变化
     */
    private fun notifyPlayerStateChanged() {
        videoController?.setPlayerState(currentState)
        mStateChangedListeners.forEach {
            it.onPlayerStateChanged(currentState)
        }
    }

    /**
     * 通知当前界面模式发生了变化
     */
    @CallSuper
    protected fun notifyScreenModeChanged(@DKVideoView.ScreenMode screenMode: Int) {
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
     * todo 目前VideoView对外可访问的回调过少，[DKPlayer.EventListener]的回调太多对外不可见
     */
    interface OnStateChangeListener {

        fun onScreenModeChanged(@DKVideoView.ScreenMode screenMode: Int) {}

        /**
         * 播放器播放状态发生了变化
         *
         * @param playState
         */
        fun onPlayerStateChanged(@PlayState playState: Int) {}

        /**
         * 播放出错
         */
        fun onPlayerError(e: Throwable) {
            println("播放器出错了：${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return playerContainer.onBackPressed()
    }

    override fun onSaveInstanceState(): Parcelable? {
        L.d("onSaveInstanceState: currentPosition=$mSeekWhenPrepared")
        //activity切到后台后可能被系统回收，故在此处进行进度保存
        saveCurrentPlayedProgress()
        return super.onSaveInstanceState()
    }//读取播放进度


    /**
     * 保存当前播放位置
     * 只会在已存在播放的情况下才会保存
     */
    private fun saveCurrentPlayedProgress() {
        val position = mSeekWhenPrepared
        if (position <= 0) return
        savePlayedProgress(mUrl, position)
    }

    private fun savePlayedProgress(url: String?, position: Long) {
        if (url.isNullOrEmpty())
            return
        progressManager?.let {
            L.d("saveProgress: $position")
            it.saveProgress(url, position)
        } ?: L.w("savePlayedProgress is ignored,ProgressManager is null.")
    }

    companion object {
        /**
         * 播放出错
         */
        const val STATE_ERROR = -1

        /**
         * 闲置中
         */
        const val STATE_IDLE = 0

        /**
         * 准备中：处于已设置了播放数据源，但是播放器还未回调[DKPlayer.EventListener.onPrepared]
         */
        const val STATE_PREPARING = 1

        /**
         * 已就绪
         */
        const val STATE_PREPARED = 2

        /**
         * 已就绪但终止状态
         * 播放过程中停止继续播放：比如手机不允许在手机流量的时候进行播放（此时播放器处于已就绪未播放中状态）
         */
        const val STATE_PREPARED_BUT_ABORT = 8

        /**
         * 播放中
         */
        const val STATE_PLAYING = 3

        /**
         * 暂停中
         */
        const val STATE_PAUSED = 4

        /**
         * 播放结束
         */
        const val STATE_PLAYBACK_COMPLETED = 5

        /**
         * 缓冲中
         */
        const val STATE_BUFFERING = 6

        /**
         * 缓冲结束
         */
        const val STATE_BUFFERED = 7

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
        playerContainer = DKVideoViewContainer(context).also {
            it.setBackgroundColor(playerBackgroundColor)
        }
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(playerContainer, params)
        playerContainer.setScreenAspectRatioType(screenAspectRatioType)
    }
}