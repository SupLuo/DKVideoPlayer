package droid.unicstar.player

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import droid.unicstar.player.controller.UCSPlayerControl
import droid.unicstar.player.player.UCSPlayer
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_BUFFERED
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_BUFFERING
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_ERROR
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_IDLE
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PAUSED
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PLAYBACK_COMPLETED
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PLAYING
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PREPARED
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PREPARED_BUT_ABORT
import droid.unicstar.player.player.UCSPlayer.Companion.STATE_PREPARING
import droid.unicstar.player.player.UCSPlayer.EventListener
import droid.unicstar.player.player.UCSPlayer.OnPlayStateChangeListener
import droid.unicstar.player.player.UCSPlayerFactory
import droid.unicstar.player.widget.AudioFocusHelper
import xyz.doikki.videoplayer.ProgressManager
import xyz.doikki.videoplayer.util.L
import xyz.doikki.videoplayer.util.PlayerUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播放器代理：只管播放器播放相关
 * 在播放器基础控制功能的情况下，提供播放器切内核切换、工厂切换、音频焦点处理、进度缓存处理、播放状态回调等相关功能
 */
open class UCSPlayerProxy(private val context: Context) : UCSPlayer, UCSPlayerControl {

    //播放器内核 即代理的主要对象
    protected var mPlayer: UCSPlayer? = null
        private set

    private var mPlayerReusable: Boolean = UCSPlayerManager.isPlayerKernelReusable

    //自定义播放器构建工厂
    private var mPlayerFactory: UCSPlayerFactory<out UCSPlayer>? = null

    //进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
    protected var mProgressManager: ProgressManager? = UCSPlayerManager.progressManager
        private set

    //OnStateChangeListener集合，保存了所有开发者设置的监听器
    private val mStateChangedListeners = CopyOnWriteArrayList<OnPlayStateChangeListener>()

    //--------- data sources ---------//
    //当前播放视频的地址
    private var mUrl: Uri? = null

    //当前视频地址的请求头
    private var mHeaders: Map<String, String>? = null

    //用于播放assets里面的视频文件
    private var mAssetFileDescriptor: AssetFileDescriptor? = null

    //--------- end data sources ---------//

    //------------一些配置-------------------//
    // recording the seek position while preparing
    private var mSeekWhenPrepared: Long = 0

    //当前播放位置：用于重新播放（或重试时）恢复之前的播放位置
    private var mCurrentPosition: Long = 0

    //是否允许使用手机流量播放
    var isPlayOnMobileNetwork: Boolean = UCSPlayerManager.isPlayOnMobileNetwork

    //音频焦点管理帮助类
    private val mAudioFocusHelper: AudioFocusHelper = AudioFocusHelper(context)

    //是否循环播放
    private var mLooping = false

    //是否静音
    private var mMute = false

    //左声道音量
    private var mLeftVolume = 1.0f

    //右声道音量
    private var mRightVolume = 1.0f

    //目标状态
    private var mTargetState: Int = STATE_IDLE

    /**
     * 当前播放器的状态
     * mCurrentState is a VideoView object's current state.
     * mTargetState is the state that a method caller intends to reach.
     * For instance, regardless the VideoView object's current state,
     * calling pause() intends to bring the object to a target state
     * of STATE_PAUSED.
     */
    @UCSPlayer.PlayState
    override var currentState: Int = STATE_IDLE
        protected set(@UCSPlayer.PlayState state) {
            if (field != state) {
                sLogd("the play state changed. old=$field new=$state")
                field = state
                //通知播放器状态发生变化
                mStateChangedListeners.forEach {
                    it.onPlayStateChanged(state)
                }
            }
        }

    val kernel: UCSPlayer? get() = mPlayer

    /**
     * 获取播放器名字
     * @return
     */
    val playerName: String
        get() {
            val className = mPlayerFactory.orDefault(UCSPlayerManager.playerFactory).javaClass.name
            return className.substring(className.lastIndexOf(".") + 1)
        }

    private var mCustomEventListener: EventListener? = null

    private val mEventListeners: CopyOnWriteArrayList<EventListener> = CopyOnWriteArrayList()

    private val mPlayerEventListener = object : EventListener {

        //视频缓冲完毕，准备开始播放时回调
        override fun onPrepared() {
            super.onPrepared()
            sLogd("onPrepared")
            currentState = STATE_PREPARED
            if (mSeekWhenPrepared > 0) {
                sLogd("onPrepared-seekTo $mSeekWhenPrepared")
                seekTo(mSeekWhenPrepared)
            }
            if (mTargetState == STATE_PLAYING) {
                sLogd("onPrepared-start")
                start()
            }
            mCustomEventListener?.onPrepared()
            mEventListeners.forEach {
                it.onPrepared()
            }
        }

        override fun onInfo(what: Int, extra: Int) {
            sLogd("onInfo(what=$what,extra=$extra)")
            super.onInfo(what, extra)
            when (what) {
                UCSPlayer.MEDIA_INFO_BUFFERING_START -> currentState = STATE_BUFFERING
                UCSPlayer.MEDIA_INFO_BUFFERING_END -> currentState = STATE_BUFFERED
                UCSPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                    currentState = STATE_PLAYING
                }
            }
            mCustomEventListener?.onInfo(what, extra)
            mEventListeners.forEach {
                it.onInfo(what, extra)
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            sLogd("onVideoSizeChanged(width=$width,height=$height)")
            super.onVideoSizeChanged(width, height)
            mCustomEventListener?.onVideoSizeChanged(width, height)
            mEventListeners.forEach {
                it.onVideoSizeChanged(width, height)
            }
        }

        override fun onCompletion() {
            sLogd("onCompletion")
            super.onCompletion()
            mSeekWhenPrepared = 0
            mCurrentPosition = 0
            //播放完成，清除进度
            sLogd("clear saved progress. url=$mUrl")
            savePlayedProgress(mUrl?.toString(), 0)
            currentState = STATE_PLAYBACK_COMPLETED
            mTargetState = STATE_PLAYBACK_COMPLETED
            mCustomEventListener?.onCompletion()
            mEventListeners.forEach {
                it.onCompletion()
            }
        }

        override fun onError(e: Throwable) {
            sLogd("onError")
            super.onError(e)
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mCustomEventListener?.onError(e)
            mEventListeners.forEach {
                it.onError(e)
            }
        }
    }

    /**
     * 自定义播放核心，继承[UCSPlayerFactory]实现自己的播放核心
     */
    override fun setPlayerFactory(playerFactory: UCSPlayerFactory<out UCSPlayer>) {
        mPlayerFactory = playerFactory
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    override fun setProgressManager(progressManager: ProgressManager?) {
        this.mProgressManager = progressManager
    }

    override fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mAudioFocusHelper.isEnable = enableAudioFocus
    }

    /**
     * 是否处于可播放状态
     */
    fun isInPlaybackState(): Boolean {
        return mPlayer != null
                && currentState != STATE_IDLE
                && currentState != STATE_ERROR
                && currentState != STATE_PLAYBACK_COMPLETED
                && currentState != STATE_PREPARING
    }

    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    override fun addOnPlayStateChangeListener(listener: OnPlayStateChangeListener) {
        mStateChangedListeners.add(listener)
    }

    /**
     * 移除某个播放状态监听
     */
    override fun removeOnPlayStateChangeListener(listener: OnPlayStateChangeListener) {
        mStateChangedListeners.remove(listener)
    }

    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        mAssetFileDescriptor = null
        mUrl = uri
        mHeaders = headers
        mSeekWhenPrepared = getSavedPlayedProgress(uri.toString())
        openVideo()
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        mUrl = null
        mAssetFileDescriptor = fd
        mSeekWhenPrepared = 0
        openVideo()
    }

    private fun openVideo() {
        try {
            val asset = mAssetFileDescriptor
            val uri = mUrl
            // not ready for playback just yet, will try again later
            if (asset == null && uri == null)
                return
            //确保播放器内核
            val player = preparePlayer()
            if (asset != null) {
                player.setDataSource(asset)
            } else if (uri != null) {
                player.setDataSource(context, uri, mHeaders)
            }
            player.prepareAsync()
            currentState = STATE_PREPARING
        } catch (e: Throwable) {
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mCustomEventListener?.onError(e)
        }
    }

    /**
     * 准备播放器内核
     */
    protected open fun preparePlayer(): UCSPlayer {
        if (!mPlayerReusable)
            releaseCurrentPlayerKernel()
        logd("[PlayerProxy] preparePlayer playerReusable=$mPlayerReusable")
        if (mPlayer != null) {
            logd("[PlayerProxy] preparePlayer use the previous player kernel; value=$mPlayer")
            mPlayer?.reset()
            return mPlayer!!
        }
        return createPlayer().also {
            it.setEventListener(mPlayerEventListener)
            it.onInit()
            it.setLooping(mLooping)
            it.setVolume(mLeftVolume, mRightVolume)
            mPlayer = it
            logd("[PlayerProxy] preparePlayer create new player kernel; value=$it")
        }
    }

    private fun releaseCurrentPlayerKernel() {
        mPlayer?.let {
            it.setEventListener(null)
            it.release()
        }
        mPlayer = null
    }

    /**
     * 创建播放器
     */
    protected open fun createPlayer(): UCSPlayer {
        return UCSPlayerManager.createMediaPlayer(context, mPlayerFactory).also {
            logd("CSPlayerProxy", "使用播放器${
                it.javaClass.name.run {
                    this.substring(this.lastIndexOf("."))
                }
            }")
        }
    }

    override fun prepareAsync() {
        requirePlayer().prepareAsync()
    }

    override fun isPlaying(): Boolean {
        return isInPlaybackState() && mPlayer?.isPlaying().orDefault()
    }

    override fun start() {
        //已就绪，准备开始播放
        if (isInPlaybackState()) {
            //移动网络不允许播放
            if (currentState == STATE_PREPARED_BUT_ABORT) {
                return
            }
            //进行移动流量播放提醒
            if (showNetworkWarning() && currentState != STATE_PREPARED_BUT_ABORT) {
                //中止播放
                currentState = STATE_PREPARED_BUT_ABORT
                return
            }
            startInPlaybackState()
        } else {
//            if (currentState == STATE_IDLE || player == null) {
//                openVideo()
//            }
            //todo 是否需要考虑错误或者播放完成的情况重播的逻辑？
            mTargetState = STATE_PLAYING
        }
    }

    override fun resume() {
        mPlayer?.let { player ->
            if (isInPlaybackState() && !player.isPlaying()) {
                startInPlaybackState()
            }
        }
        mTargetState = STATE_PLAYING
    }

    /**
     * 播放状态下开始播放
     */
    protected open fun startInPlaybackState() {
        requirePlayer().start()
        if (!mMute) {
            mAudioFocusHelper.requestFocus()
        }
        currentState = STATE_PLAYING
    }

    override fun getCurrentPosition(): Long {
        return if (isInPlaybackState()) {
            requirePlayer().getCurrentPosition().also {
                mCurrentPosition = it
            }
        } else 0
    }

    override fun getDuration(): Long {
        return if (isInPlaybackState()) {
            mPlayer?.getDuration().orDefault(-1)
        } else -1
    }

    override fun getBufferedPercentage(): Int {
        return mPlayer?.getBufferedPercentage().orDefault()
    }

    override fun setLooping(isLooping: Boolean) {
        mLooping = isLooping
        mPlayer?.setLooping(isLooping)
    }

    override fun setMute(isMute: Boolean) {
        mMute = isMute
        mAudioFocusHelper.isPlayerMute = isMute
        mPlayer?.let { player ->
            val leftVolume = if (isMute) 0.0f else mLeftVolume
            val rightVolume = if (isMute) 0.0f else mRightVolume
            player.setVolume(leftVolume, rightVolume)
        }
    }

    override fun isMute(): Boolean {
        return mMute
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        mLeftVolume = leftVolume
        mRightVolume = rightVolume
        mPlayer?.setVolume(leftVolume, rightVolume)
    }

    override fun setPlayerReusable(reusable: Boolean) {
        mPlayerReusable = reusable
    }

    override fun getSpeed(): Float {
        return if (isInPlaybackState()) {
            mPlayer?.getSpeed().orDefault(1f)
        } else 1f
    }

    override fun setSpeed(speed: Float) {
        if (isInPlaybackState()) {
            mPlayer?.setSpeed(speed)
        }
    }

    override fun getTcpSpeed(): Long {
        return mPlayer?.getTcpSpeed().orDefault()
    }

    override fun seekTo(msec: Long) {
        mSeekWhenPrepared = if (isInPlaybackState()) {
            mPlayer?.seekTo(msec)
            0
        } else {
            msec
        }
    }

    override fun pause() {
        mPlayer?.let { player ->
            if (isInPlaybackState() && player.isPlaying()) {
                player.pause()
                currentState = STATE_PAUSED
                if (!mMute) {
                    mAudioFocusHelper.abandonFocus()
                }
            }
        }
        mTargetState = STATE_PAUSED
    }

    override fun replay(resetPosition: Boolean) {
        //用于恢复之前播放的位置
        if (!resetPosition && mCurrentPosition > 0) {
            mSeekWhenPrepared = mCurrentPosition
        }
        openVideo()
        //todo
//        //重新设置option，media player reset之后，option会失效
//        preparePlayerOptions()
//        attachMediaController()
//        prepareKernelDataSource()
        start()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        mPlayer?.reset()
    }

    override fun release() {
        if (currentState != STATE_IDLE) {
            //释放播放器
            mPlayer?.release()
            mPlayer = null
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

//            //todo 清除所有的回调监听？
//            mStateChangedListeners.clear()
        }
    }

    /**
     * 保存当前播放位置
     * 只会在已存在播放的情况下才会保存
     */
    private fun saveCurrentPlayedProgress() {
        val position = mCurrentPosition
        if (position <= 0) return
        savePlayedProgress(mUrl?.toString(), position)
    }

    private fun savePlayedProgress(url: String?, position: Long) {
        if (url.isNullOrEmpty())
            return
        mProgressManager?.let {
            L.d("saveProgress: $position")
            it.saveProgress(url, position)
        } ?: L.w("savePlayedProgress is ignored,ProgressManager is null.")
    }

    override fun setSurface(surface: Surface?) {
        mPlayer?.setSurface(surface)
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        mPlayer?.setDisplay(holder)
    }

    fun addEventListener(eventListener: EventListener) {
        this.mEventListeners.add(eventListener)
    }

    fun removeEventListener(eventListener: EventListener) {
        this.mEventListeners.remove(eventListener)
    }

    override fun setEventListener(eventListener: EventListener?) {
        this.mCustomEventListener = eventListener
    }

    /**
     * release the media player in any state
     */
    fun releasePlayer(clearTargetState: Boolean) {
        mPlayer?.let {
            it.reset()
            it.release()
            mPlayer = null
            currentState = STATE_IDLE
            if (clearTargetState) {
                mTargetState = STATE_IDLE
            }
            mAudioFocusHelper.abandonFocus()
        }
    }

    internal fun requirePlayer(): UCSPlayer {
        return mPlayer ?: throw IllegalStateException("请先创建播放器（prepareMediaPlayer）")
    }

    /**
     * 获取已保存的当前播放进度
     *
     * @return
     */
    private fun getSavedPlayedProgress(url: String): Long {
        return mProgressManager?.getSavedProgress(url).orDefault()
    }

    /**
     * 是否显示流量播放提示
     */
    private fun showNetworkWarning(): Boolean {
        //非本地视频源，并且不允许使用流量播放，并且当前网络是手机流量的情况下，进行网络提示
        return !isLocalDataSource && !isPlayOnMobileNetwork && PlayerUtils.getNetworkType(context) == PlayerUtils.NETWORK_MOBILE
    }

    /**
     * 判断是否为本地数据源，包括 本地文件、Asset、raw
     */
    private val isLocalDataSource: Boolean
        get() {
            if (mAssetFileDescriptor != null) {
                return true
            }
            val uri = mUrl
            if (uri != null) {
                return ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme || ContentResolver.SCHEME_FILE == uri.scheme || "rawresource" == uri.scheme
            }
            return false
        }

    private fun sLogd(message: String) {
        logd("[UNSPlayerProxy]:$message")
    }
}