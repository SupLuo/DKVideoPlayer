package droid.unicstar.videoplayer.player

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import droid.unicstar.videoplayer.logd
import droid.unicstar.videoplayer.orDefault
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_ERROR
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_IDLE
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PAUSED
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PLAYBACK_COMPLETED
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PLAYING
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PREPARED_BUT_ABORT
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PREPARING
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_PREPARED
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_BUFFERING
import droid.unicstar.videoplayer.player.CSPlayer.Companion.STATE_BUFFERED
import droid.unicstar.videoplayer.player.CSPlayer.EventListener
import droid.unicstar.videoplayer.player.CSPlayer.OnPlayStateChangeListener
import droid.unicstar.videoplayer.tryIgnore
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.ProgressManager
import xyz.doikki.videoplayer.internal.AudioFocusHelper
import xyz.doikki.videoplayer.util.L
import xyz.doikki.videoplayer.util.PlayerUtils
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播放器代理：只管播放器播放相关
 */
open class CSPlayerProxy(private val context: Context) : CSPlayer {

    //播放器内核 即代理的主要对象
    protected var player: CSPlayer? = null
        private set

    //自定义播放器构建工厂
    private var mPlayerFactory: CSPlayerFactory<out CSPlayer>? = null

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
    var isPlayOnMobileNetwork: Boolean = DKManager.isPlayOnMobileNetwork
    //音频焦点管理帮助类
    private val mAudioFocusHelper: AudioFocusHelper = AudioFocusHelper(this)
    //是否循环播放
    private var mLooping = false
    //是否静音
    private var mMute = false
    //左声道音量
    private var mLeftVolume = 1.0f
    //右声道音量
    private var mRightVolume = 1.0f
    /**
     * 进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
     */
    protected var progressManager: ProgressManager? = DKManager.progressManager
        private set

    /**
     * 当前播放器的状态
     * mCurrentState is a VideoView object's current state.
     * mTargetState is the state that a method caller intends to reach.
     * For instance, regardless the VideoView object's current state,
     * calling pause() intends to bring the object to a target state
     * of STATE_PAUSED.
     */
    @CSPlayer.PlayState
    var currentState: Int = STATE_IDLE
        protected set(@CSPlayer.PlayState state) {
            if (field != state) {
                field = state
                notifyPlayerStateChanged()
            }
        }

    //目标状态
    private var mTargetState: Int = STATE_IDLE

    /**
     * 获取播放器名字
     * @return
     */
    val playerName: String
        get() {
            val className = mPlayerFactory.orDefault(DKManager.playerFactory).javaClass.name
            return className.substring(className.lastIndexOf(".") + 1)
        }

    private var mCustomEventListener: EventListener? = null

    private val mEventListener = object : EventListener {

        //视频缓冲完毕，准备开始播放时回调
        override fun onPrepared() {
            super.onPrepared()
            currentState = STATE_PREPARED
            if (mSeekWhenPrepared > 0) {
                seekTo(mSeekWhenPrepared)
            }
            if (mTargetState == STATE_PLAYING) {
                start()
            }
            mCustomEventListener?.onPrepared()
        }

        override fun onInfo(what: Int, extra: Int) {
            super.onInfo(what, extra)
            when (what) {
                CSPlayer.MEDIA_INFO_BUFFERING_START -> currentState = STATE_BUFFERING
                CSPlayer.MEDIA_INFO_BUFFERING_END -> currentState = STATE_BUFFERED
                CSPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                    currentState = STATE_PLAYING
                }
                CSPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> setRotation(extra)
            }
            mCustomEventListener?.onInfo(what, extra)
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            super.onVideoSizeChanged(width, height)
            mCustomEventListener?.onVideoSizeChanged(width, height)
        }

        override fun onCompletion() {
            super.onCompletion()
            mSeekWhenPrepared = 0
            mCurrentPosition = 0
            //播放完成，清除进度
            savePlayedProgress(mUrl?.toString(), 0)
            currentState = STATE_PLAYBACK_COMPLETED
            mTargetState = STATE_PLAYBACK_COMPLETED
        }

        override fun onError(e: Throwable) {
            super.onError(e)
            currentState = STATE_ERROR
            mTargetState = STATE_ERROR
            mCustomEventListener?.onError(e)
            mStateChangedListeners.forEach {
                it.onPlayError(e)
            }
        }
    }

    /**
     * 自定义播放核心，继承[CSPlayerFactory]实现自己的播放核心
     */
    fun setPlayerFactory(playerFactory: CSPlayerFactory<out CSPlayer>) {
        mPlayerFactory = playerFactory
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    fun setProgressManager(progressManager: ProgressManager?) {
        this.progressManager = progressManager
    }

    /**
     * 是否开启AudioFocus监听， 默认开启，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mAudioFocusHelper.isEnable = enableAudioFocus
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
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    fun addOnPlayStateChangeListener(listener: OnPlayStateChangeListener) {
        mStateChangedListeners.add(listener)
    }

    /**
     * 移除某个播放状态监听
     */
    fun removeOnPlayStateChangeListener(listener: OnPlayStateChangeListener) {
        mStateChangedListeners.remove(listener)
    }

    /**
     * 移除所有播放状态监听
     */
    fun clearPlayStateChangeListeners() {
        mStateChangedListeners.clear()
    }

    /**
     * release the media player in any state
     */
    fun releasePlayer(clearTargetState: Boolean) {
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

    internal fun requirePlayer(): CSPlayer {
        return player ?: throw IllegalStateException("请先创建播放器（prepareMediaPlayer）")
    }

    /**
     * 判断是否为本地数据源，包括 本地文件、Asset、raw
     */
    internal val isLocalDataSource: Boolean
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

    /**
     * 设置静音
     *
     * @param isMute true:静音 false：相反
     */
    fun setMute(isMute: Boolean) {
        mMute = isMute
        player?.let { player ->
            val leftVolume = if (isMute) 0.0f else mLeftVolume
            val rightVolume = if (isMute) 0.0f else mRightVolume
            player.setVolume(leftVolume, rightVolume)
        }
    }

    /**
     * 是否处于静音中
     */
    fun isMute(): Boolean {
        return mMute
    }

    /**
     * 通知播放器状态发生变化
     */
    private fun notifyPlayerStateChanged() {
        mStateChangedListeners.forEach {
            it.onPlayStateChanged(currentState)
        }
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
            mStateChangedListeners.forEach {
                it.onPlayError(e)
            }
        }
    }

    /**
     * 确保播放器可用
     */
    protected open fun preparePlayer(): CSPlayer {
        //目前每次都重新创建一个播放器
        player?.release()
        player = createPlayer().also {
            it.setEventListener(mEventListener)
            it.init()
            it.setLooping(mLooping)
        }
        return player!!
    }

    /**
     * 创建播放器
     */
    protected open fun createPlayer(): CSPlayer {
        return DKManager.createMediaPlayer(context, mPlayerFactory).also {
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
        return isInPlaybackState() && player?.isPlaying().orDefault()
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
            mTargetState = STATE_PLAYING
        }
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


    /**
     * 是否显示流量播放提示
     */
    private fun showNetworkWarning(): Boolean {
        //非本地视频源，并且不允许使用流量播放，并且当前网络是手机流量的情况下，进行网络提示
        return !isLocalDataSource && !isPlayOnMobileNetwork && PlayerUtils.getNetworkType(context) == PlayerUtils.NETWORK_MOBILE
    }

    override fun getCurrentPosition(): Long {
        if (isInPlaybackState()) {
            mCurrentPosition = requirePlayer().getCurrentPosition()
            return mCurrentPosition
        }
        return 0
    }

    override fun getDuration(): Long {
        return if (isInPlaybackState()) {
            player?.getDuration().orDefault()
        } else -1
    }

    override fun getBufferedPercentage(): Int {
        return player?.getBufferedPercentage().orDefault()
    }

    override fun setLooping(isLooping: Boolean) {
        mLooping = isLooping
        player?.setLooping(isLooping)
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        mLeftVolume = leftVolume
        mRightVolume = rightVolume
        player?.setVolume(leftVolume, rightVolume)
    }

    override fun getSpeed(): Float {
        return if (isInPlaybackState()) {
            player?.getSpeed().orDefault(1f)
        } else 1f
    }

    override fun setSpeed(speed: Float) {
        if (isInPlaybackState()) {
            player?.setSpeed(speed)
        }
    }

    override fun getTcpSpeed(): Long {
        return player?.getTcpSpeed().orDefault()
    }

    override fun seekTo(msec: Long) {
        mSeekWhenPrepared = if (isInPlaybackState()) {
            player?.seekTo(msec)
            0
        } else {
            msec
        }
    }

    override fun pause() {
        player?.let { player ->
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

    fun resume(){
        player?.let { player ->
            if (isInPlaybackState() && !player.isPlaying()) {
                player.start()
                currentState = STATE_PLAYING
                if (!mMute) {
                    mAudioFocusHelper.requestFocus()
                }
            }
        }
        mTargetState = STATE_PLAYING
    }

    fun replay(resetPosition: Boolean){
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


    override fun stop() {
        player?.stop()
    }

    override fun reset() {
        player?.reset()
    }

    override fun release() {
        if (currentState != STATE_IDLE) {
            //释放播放器
            player?.release()
            player = null
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


     fun stopPlayback() {
        //释放播放器
        player?.release()
        player = null
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
     * 保存当前播放位置
     * 只会在已存在播放的情况下才会保存
     */
    private fun saveCurrentPlayedProgress() {
        val position = mSeekWhenPrepared
        if (position <= 0) return
        savePlayedProgress(mUrl?.toString(), position)
    }

    private fun savePlayedProgress(url: String?, position: Long) {
        if (url.isNullOrEmpty())
            return
        progressManager?.let {
            L.d("saveProgress: $position")
            it.saveProgress(url, position)
        } ?: L.w("savePlayedProgress is ignored,ProgressManager is null.")
    }

    override fun setSurface(surface: Surface?) {
        player?.setSurface(surface)
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        player?.setDisplay(holder)
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
        return progressManager?.getSavedProgress(url).orDefault()
    }
}