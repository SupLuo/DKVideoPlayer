package unics.player.kernel

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.CallSuper
import unics.player.UCSPlayerManager
import unics.player.internal.plogd2
import unics.player.internal.plogi2
import unics.player.kernel.UCSPlayer.Companion.STATE_BUFFERED
import unics.player.kernel.UCSPlayer.Companion.STATE_BUFFERING
import unics.player.kernel.UCSPlayer.Companion.STATE_ERROR
import unics.player.kernel.UCSPlayer.Companion.STATE_IDLE
import unics.player.kernel.UCSPlayer.Companion.STATE_PAUSED
import unics.player.kernel.UCSPlayer.Companion.STATE_PLAYBACK_COMPLETED
import unics.player.kernel.UCSPlayer.Companion.STATE_PLAYING
import unics.player.kernel.UCSPlayer.Companion.STATE_PREPARED
import unics.player.kernel.UCSPlayer.Companion.STATE_PREPARED_BUT_ABORT
import unics.player.kernel.UCSPlayer.Companion.STATE_PREPARING
import unics.player.kernel.UCSPlayer.EventListener
import unics.player.kernel.UCSPlayer.OnPlayStateChangeListener
import unics.player.tryIgnore
import unics.player.widget.AudioFocusHelper
import xyz.doikki.videoplayer.ProgressManager
import xyz.doikki.videoplayer.util.PlayerUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播放器代理：只管播放器播放相关
 * 本类设计：播放器代理类，代理与播放相关的所有内容，即本类为播放相关的核心实现
 * 在播放器基础控制功能的情况下，提供播放器切内核切换、工厂切换、音频焦点处理、进度缓存处理、播放状态回调等相关功能
 */
open class PlayerProxy(private val context: Context) : UCSPlayer, UCSPlayerControl {

    private val mLogPrefix = "[PlayerProxy@${this.hashCode()}]"

    //播放器内核 即代理的主要对象
    private var mPlayer: UCSPlayer? = null

    //player 是否重用，即每次播放是否重新使用新的实例
    private var mReusable: Boolean = UCSPlayerManager.isPlayerKernelReusable

    //自定义播放器构建工厂
    private var mFactory: PlayerFactory<out UCSPlayer>? = null

    //进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
    private var mProgressManager: ProgressManager? = UCSPlayerManager.progressManager

    //OnStateChangeListener集合，保存了所有开发者设置的监听器
    private val mStateChangedListeners = CopyOnWriteArrayList<OnPlayStateChangeListener>()

    //是否允许使用手机流量播放
    var isPlayOnMobileNetwork: Boolean = UCSPlayerManager.isPlayOnMobileNetwork

    //音频焦点管理帮助类
    private val mAudioFocusHelper: AudioFocusHelper = AudioFocusHelper(context)

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
                plogi2(mLogPrefix) { "the play state changed(old=$field new=$state),notify." }
                field = state
                //通知播放器状态发生变化
                mStateChangedListeners.forEach {
                    it.onPlayStateChanged(state)
                }
            }
        }

    val player: UCSPlayer? get() = mPlayer

    fun requirePlayer(): UCSPlayer {
        return mPlayer ?: throw IllegalStateException("请先创建播放器（prepareMediaPlayer）")
    }

    /**
     * 获取播放器名字
     * @return
     */
    val playerName: String
        get() {
            val className = (mFactory ?: UCSPlayerManager.playerFactory).javaClass.name
            return className.substring(className.lastIndexOf(".") + 1)
        }

    //外层设置的事件监听
    private var mCustomEventListener: EventListener? = null

    private val mEventListeners: CopyOnWriteArrayList<EventListener> = CopyOnWriteArrayList()

    private val mPlayerEventListener = object : EventListener {

        //视频缓冲完毕，准备开始播放时回调
        override fun onPrepared() {
            plogi2(mLogPrefix) { "onPrepared" }
            currentState = STATE_PREPARED
            if (mSeekWhenPrepared > 0) {
                plogi2(mLogPrefix) { "onPrepared -> seekWhenPrepared(=$mSeekWhenPrepared) > 0,seek to." }
                seekTo(mSeekWhenPrepared)
            }
            mCustomEventListener?.onPrepared()
            mEventListeners.forEach {
                it.onPrepared()
            }
            if (mTargetState == STATE_PLAYING) {
                plogi2(mLogPrefix) { "onPrepared -> target state is playing,invoke start." }
                start()
            }
        }

        override fun onInfo(what: Int, extra: Int) {
            plogi2(mLogPrefix) { "onInfo(what=$what, extra=$extra)" }
            when (what) {
                UCSPlayer.MEDIA_INFO_BUFFERING_START -> currentState = STATE_BUFFERING
                UCSPlayer.MEDIA_INFO_BUFFERING_END -> currentState = STATE_BUFFERED
                UCSPlayer.MEDIA_INFO_RENDERING_PREPARED -> {
                    currentState = STATE_PLAYING
                }
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
            plogi2(mLogPrefix) { "onVideoSizeChanged(width=$width, height=$height)" }
            mCustomEventListener?.onVideoSizeChanged(width, height)
            mEventListeners.forEach {
                it.onVideoSizeChanged(width, height)
            }
        }

        override fun onCompletion() {
            mSeekWhenPrepared = 0
            mCurrentPosition = 0
            plogi2(mLogPrefix) { "onCompletion,reset field[seekWhenPrepared(=$mSeekWhenPrepared),currentPosition(=$mCurrentPosition),savePlayedProgress] to 0." }
            //播放完成，清除进度
            savePlayedProgress(mUrl?.toString(), 0)
            currentState = STATE_PLAYBACK_COMPLETED
            mTargetState = STATE_PLAYBACK_COMPLETED
            mCustomEventListener?.onCompletion()
            mEventListeners.forEach {
                it.onCompletion()
            }
        }

        override fun onError(e: Throwable) {
            plogi2(mLogPrefix) { "onError" }
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mCustomEventListener?.onError(e)
            mEventListeners.forEach {
                it.onError(e)
            }
        }
    }

    /**
     * 自定义播放核心，继承[PlayerFactory]实现自己的播放核心
     */
    override fun setPlayerFactory(playerFactory: PlayerFactory<out UCSPlayer>) {
        plogd2(mLogPrefix) { "setPlayerFactory($playerFactory)" }
        mFactory = playerFactory
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    override fun setProgressManager(progressManager: ProgressManager?) {
        plogd2(mLogPrefix) { "setProgressManager($progressManager)" }
        this.mProgressManager = progressManager
    }

    override fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        plogd2(mLogPrefix) { "setEnableAudioFocus($enableAudioFocus)" }
        mAudioFocusHelper.isEnable = enableAudioFocus
    }

    /**
     * 是否处于可播放状态
     */
    override fun isInPlaybackState(): Boolean {
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
        plogd2(mLogPrefix) { "addOnPlayStateChangeListener($listener)" }
        mStateChangedListeners.add(listener)
    }

    /**
     * 移除某个播放状态监听
     */
    override fun removeOnPlayStateChangeListener(listener: OnPlayStateChangeListener) {
        plogd2(mLogPrefix) { "removeOnPlayStateChangeListener($listener)" }
        mStateChangedListeners.remove(listener)
    }

    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        plogi2(mLogPrefix) { "setDataSource(Context, uri=$uri, headers=$headers)" }
        mAssetFileDescriptor = null
        mUrl = uri
        mHeaders = headers
        mSeekWhenPrepared = getSavedPlayedProgress(uri.toString())
        plogi2(mLogPrefix) { "get saved played progress = $mSeekWhenPrepared" }
        openVideo()
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        plogi2(mLogPrefix) { "setDataSource(AssetFileDescriptor=$fd)" }
        mUrl = null
        mAssetFileDescriptor = fd
        mSeekWhenPrepared = 0
        openVideo()
    }

    private fun openVideo() {
        try {
            plogi2(mLogPrefix) { "openVideo" }
            val asset = mAssetFileDescriptor
            val uri = mUrl
            // not ready for playback just yet, will try again later
            if (asset == null && uri == null) {
                plogi2(mLogPrefix) { "openVideo -> not ready for playback just yet, asset and uri is null." }
                return
            }
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
    private fun preparePlayer(): UCSPlayer {
        plogi2(mLogPrefix) { "preparePlayer , reusable = $mReusable" }
        if (!mReusable) {
            plogi2(mLogPrefix) { "preparePlayer -> player set not reusable,release current player if not null （current = ${mPlayer}）." }
            releasePlayer(false)
        }
        val player = mPlayer
        if (player != null) {//todo 如果重用播放器，此处该不该执行reset？是否应该交给用户调用
            plogi2(mLogPrefix) { "preparePlayer -> reuse player($player),reset it before use." }
            player.reset()
            return player
        }
        return UCSPlayerManager.createMediaPlayer(context, mFactory).also {
            plogi2(mLogPrefix) { "preparePlayer -> created new player kernel $it,init it." }
            setupPlayer(it)
            mPlayer = it
            plogi2(mLogPrefix) { "preparePlayer -> new player kernel created, value=$it" }
        }
    }

    @CallSuper
    protected open fun setupPlayer(player: UCSPlayer) {
        plogi2(mLogPrefix) { "setupPlayer" }
        player.setEventListener(mPlayerEventListener)
        player.setLooping(mLooping)
        player.setVolume(mLeftVolume, mRightVolume)
    }

    override fun prepareAsync() {
        plogi2(mLogPrefix) { "prepareAsync" }
        requirePlayer().prepareAsync()
    }

    override fun isPlaying(): Boolean {
        return isInPlaybackState() && mPlayer?.isPlaying() ?: false
    }

    override fun start() {
        plogi2(mLogPrefix) { "start: playerName=$playerName reusable=${mReusable} looping=$mLooping mute=$mMute leftVol=$mLeftVolume rightVol=$mRightVolume progressManager=$mProgressManager" }
        //已就绪，准备开始播放
        if (isInPlaybackState()) {
            plogi2(mLogPrefix) { "start -> isInPlaybackState" }
            //移动网络不允许播放
            if (currentState == STATE_PREPARED_BUT_ABORT) {
                plogi2(mLogPrefix) { "start -> currentState == STATE_PREPARED_BUT_ABORT return." }
                return
            }
            //进行移动流量播放提醒
            if (showNetworkWarning() && currentState != STATE_PREPARED_BUT_ABORT) {
                plogi2(mLogPrefix) { "start -> showNetworkWarning() && currentState != STATE_PREPARED_BUT_ABORT return." }
                //中止播放
                currentState = STATE_PREPARED_BUT_ABORT
                return
            }
            plogi2(mLogPrefix) { "start -> startInPlaybackState" }
            startInPlaybackState()
        } else {
            plogi2(mLogPrefix) { "start -> mTargetState = STATE_PLAYING" }
            mTargetState = STATE_PLAYING
        }
    }

    override fun resume() {
        plogi2(mLogPrefix) { "resume" }
        mPlayer?.let { player ->
            if (isInPlaybackState() && !player.isPlaying()) {
                plogi2(mLogPrefix) { "resume -> startInPlaybackState" }
                startInPlaybackState()
            }
        }
        mTargetState = STATE_PLAYING
    }

    /**
     * 播放状态下开始播放
     */
    protected open fun startInPlaybackState() {
        plogi2(mLogPrefix) { "startInPlaybackState ->  requirePlayer().start()" }
        requirePlayer().start()
        if (!mMute) {
            plogi2(mLogPrefix) { "startInPlaybackState -> request audio focus." }
            mAudioFocusHelper.requestFocus()
        }
        currentState = STATE_PLAYING
    }

    override fun pause() {
        plogd2(mLogPrefix) { "pause" }
        mPlayer?.let { player ->
            if (isInPlaybackState() && player.isPlaying()) {
                plogd2(mLogPrefix) { "pause -> invoke." }
                player.pause()
                currentState = STATE_PAUSED
                if (!mMute) {
                    plogd2(mLogPrefix) { "pause -> abandonFocus audio focus." }
                    mAudioFocusHelper.abandonFocus()
                }
            }
        }
        mTargetState = STATE_PAUSED
    }

    override fun replay(resetPosition: Boolean) {
        plogd2(mLogPrefix) { "replay" }
        //用于恢复之前播放的位置
        if (!resetPosition && mCurrentPosition > 0) {
            mSeekWhenPrepared = mCurrentPosition
        }
        openVideo()
        start()
    }

    override fun stop() {
        plogd2(mLogPrefix) { "stop" }
        mPlayer?.let {
            if (mReusable) {
                plogd2(mLogPrefix) { "stop -> player kernel set reusable, call the player stop method." }
                it.stop()
            } else {
                plogd2(mLogPrefix) { "stop -> player kernel set un reusable, try release it directly." }
                releasePlayer(true)
            }
        }
        currentState = STATE_IDLE
    }

    override fun reset() {
        plogd2(mLogPrefix) { "reset" }
        mPlayer?.let {
            if (mReusable) {
                plogd2(mLogPrefix) { "reset -> player kernel set reusable, call the player reset method." }
                it.reset()
            } else {
                plogd2(mLogPrefix) { "reset -> player kernel set un reusable, try release it directly." }
                releasePlayer(true)
            }
        }
        currentState = STATE_IDLE
    }

    override fun release() {
        plogd2(mLogPrefix) { "release" }
        if (currentState != STATE_IDLE) {
            //释放Assets资源
            mAssetFileDescriptor?.let {
                tryIgnore {
                    it.close()
                }
            }
            //保存播放进度
            saveCurrentPlayedProgress()
            //重置播放进度
            mSeekWhenPrepared = 0
            mCurrentPosition = 0
            //释放播放器
            releasePlayer(true)
            //切换转态
            currentState = STATE_IDLE
            mTargetState = STATE_IDLE
            mStateChangedListeners.clear()
        }
    }

    /**
     * 释放当前持有的播放器
     */
    fun releasePlayer(resetTargetState: Boolean) {
        plogi2(mLogPrefix) { "releasePlayer" }
        mPlayer?.let {
            plogi2(mLogPrefix) { "releasePlayer -> before release: currentState=$currentState,targetState=$mTargetState" }
            it.setEventListener(null)
            it.release()
            if (currentState != STATE_IDLE && currentState != STATE_ERROR && currentState != STATE_PLAYBACK_COMPLETED) {
                currentState = STATE_IDLE
            }
            if (resetTargetState) {
                mTargetState = STATE_IDLE
            }
            //放弃音频焦点
            mAudioFocusHelper.abandonFocus()
            plogi2(mLogPrefix) { "releasePlayer -> after release: currentState=$currentState,targetState=$mTargetState" }
        }
        mPlayer = null
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
            mPlayer?.getDuration() ?: -1
        } else -1
    }

    override fun getBufferedPercentage(): Int {
        return mPlayer?.getBufferedPercentage() ?: 0
    }

    override fun setLooping(looping: Boolean) {
        mLooping = looping
        mPlayer?.setLooping(looping)
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
        plogd2(mLogPrefix) { "setPlayerReusable($reusable)" }
        mReusable = reusable
    }

    override fun getSpeed(): Float {
        return if (isInPlaybackState()) {
            mPlayer?.getSpeed() ?: 1f
        } else 1f
    }

    override fun setSpeed(speed: Float) {
        if (isInPlaybackState()) {
            plogd2(mLogPrefix) { "setSpeed($speed)" }
            mPlayer?.setSpeed(speed)
        }
    }

    override fun getTcpSpeed(): Long {
        return mPlayer?.getTcpSpeed() ?: 0
    }

    override fun seekTo(msec: Long) {
        mSeekWhenPrepared = if (isInPlaybackState()) {
            plogd2(mLogPrefix) { "seekTo($msec) -> is in playback state,invoke seek." }
            mPlayer?.seekTo(msec)
            0
        } else {
            plogd2(mLogPrefix) { "seekTo($msec) -> is not in playback state,set seekWhenPrepare = $msec." }
            msec
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
        if (url.isNullOrEmpty()) {
            plogi2(mLogPrefix) { "savePlayedProgress -> ignore save progress(url is null or empty)." }
            return
        }
        val pm = mProgressManager
        if (pm == null) {
            plogi2(mLogPrefix) { "savePlayedProgress -> ignore save progress(progress manager is null)." }
            return
        }
        plogd2(mLogPrefix) { "savePlayedProgress -> saveProgress(position=$position,url=$url)" }
        pm.saveProgress(url, position)
    }

    override fun setSurface(surface: Surface?) {
        plogd2(mLogPrefix) { "setSurface($surface)" }
        //用可空的对象来设置，因为存在surface在destroy的时候调用player设置为null
        mPlayer?.setSurface(surface)
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        plogd2(mLogPrefix) { "setDisplay($holder)" }
        //用可空的对象来设置，因为存在surface在destroy的时候调用player设置为null
        mPlayer?.setDisplay(holder)
    }

    override fun addEventListener(eventListener: EventListener) {
        this.mEventListeners.add(eventListener)
    }

    override fun removeEventListener(eventListener: EventListener) {
        this.mEventListeners.remove(eventListener)
    }

    override fun setEventListener(eventListener: EventListener?) {
        this.mCustomEventListener = eventListener
    }

    /**
     * 获取已保存的当前播放进度
     *
     * @return
     */
    private fun getSavedPlayedProgress(url: String): Long {
        return mProgressManager?.getSavedProgress(url) ?: 0
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

}