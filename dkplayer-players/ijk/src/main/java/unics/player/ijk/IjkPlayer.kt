package unics.player.ijk

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer.OnNativeInvokeListener
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import unics.player.UCSPManager
import unics.player.internal.plogi2
import unics.player.kernel.BasePlayer
import unics.player.kernel.PlayerException
import unics.player.kernel.UCSPlayer

open class IjkPlayer(private val appContext: Context) : BasePlayer(),
    IMediaPlayer.OnErrorListener,
    IMediaPlayer.OnCompletionListener,
    IMediaPlayer.OnInfoListener,
    IMediaPlayer.OnBufferingUpdateListener,
    IMediaPlayer.OnPreparedListener,
    IMediaPlayer.OnVideoSizeChangedListener,
    OnNativeInvokeListener {

    private var mMediacodec: Boolean = UCSPManager.isMediacodec

    private var mBufferedPercent = 0

    /**
     * 播放器真正的内核
     */
    @JvmField
    protected var mKernel: IjkMediaPlayer? = null

    fun requirePlayer(): IjkMediaPlayer {
        ilogv { "requirePlayer." }
        return mKernel ?: createKernel().also {
            mKernel = it
        }
    }

    fun getPlayer(): IjkMediaPlayer? = mKernel

    override fun setMediacodec(enable: Boolean) {
        mMediacodec = enable
    }

    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        try {
            ilogi { "setDataSource(Context,uri=$uri,headers=$headers)" }
            requirePlayerBlock { player ->
                if (ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme) {
                    ilogi { "setDataSource(Uri) android resource ,transform to RawDataSourceProvider." }
                    val rawDataSourceProvider = RawDataSourceProvider.create(appContext, uri)
                    player.setDataSource(rawDataSourceProvider)
                } else {
                    if (headers?.containsKey("User-Agent") == true) {
                        ilogi { "setDataSource(Uri) headers contain 'User-Agent'" }
                        //处理UA问题
                        //update by luochao: 直接在Map参数中移除字段，可能影响调用者的逻辑
                        val clonedHeaders: MutableMap<String, String> = HashMap(headers)
                        // 移除header中的User-Agent，防止重复
                        val userAgent = clonedHeaders.remove("User-Agent")
                        player.setOption(
                            IjkMediaPlayer.OPT_CATEGORY_FORMAT,
                            "user_agent",
                            userAgent
                        )
                        ilogi { "setDataSource(Uri) set user agent to ijk player option (userAgent=${userAgent})" }
                        player.setDataSource(appContext, uri, clonedHeaders)
                    } else {
                        //不包含UA，直接设置
                        ilogi { "setDataSource(Uri) doest not contain 'User-Agent',set data source directly" }
                        player.setDataSource(appContext, uri, headers)
                    }
                }
            }

        } catch (e: Throwable) {
            iloge(e) { "setDataSource(Uri) error." }
            mEventListener?.onError(e)
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        try {
            ilogi { "setDataSource(AssetFileDescriptor)" }
            requirePlayer().setDataSource(RawDataSourceProvider(fd))
        } catch (e: Throwable) {
            iloge(e) { "setDataSource(AssetFileDescriptor) error." }
            mEventListener?.onError(e)
        }
    }

    override fun pause() {
        try {
            ilogi { "pause isPlaying=${isPlaying()}" }
            mKernel?.pause()
        } catch (e: Throwable) {
            iloge(e) { "pause error." }
            mEventListener?.onError(e)
        }
    }

    override fun start() {
        try {
            ilogi { "start" }
            requirePlayerBlock {
                // 避免播放纯音频时不知道渲染开始的问题
                it.start()
                if (!isVideoSource()) {
                    iloge { "start -> 开始播放回调兼容：如果不是视频资源，则回调MEDIA_INFO_RENDERING_PREPARED" }
                    mEventListener?.onInfo(UCSPlayer.MEDIA_INFO_RENDERING_PREPARED, 0)
                }
            }
        } catch (e: Throwable) {
            iloge(e) { "start error." }
            mEventListener?.onError(e)
        }
    }

    override fun stop() {
        try {
            ilogi { "stop" }
            mKernel?.stop()
        } catch (e: Throwable) {
            iloge(e) { "stop error." }
            mEventListener?.onError(e)
        }
    }

    override fun prepareAsync() {
        try {
            ilogi { "prepareAsync" }
            mBufferedPercent = 0
            requirePlayerBlock { player ->
                //需要手动调用开始播放
                if (mMediacodec) {
                    player.applyMediacodecPreferredOptions()
                    ilogi { "prepareAsync -> use mediacodec" }
                } else {
                    player.applySoftDecodingPreferredOptions()
                    ilogi { "prepareAsync -> use soft decoding" }
                }
                player.setAutoPlayOnPrepared(false)
                player.prepareAsync()
                ilogi { "invoke prepareAsync" }
            }
        } catch (e: Throwable) {
            iloge(e) { "prepareAsync error." }
            mEventListener!!.onError(e)
        }
    }

    override fun reset() {
        mBufferedPercent = 0
        mKernel?.let {
            ilogi { "reset" }
            it.reset()
            it.setOnVideoSizeChangedListener(this)
        }
    }

    override fun isPlaying(): Boolean {
        return mKernel?.isPlaying ?: false
    }

    override fun seekTo(msec: Long) {
        try {
            ilogi { "seekTo($msec)" }
            requirePlayer().seekTo(msec.toInt().toLong())
        } catch (e: Throwable) {
            iloge(e) { "seekTo error." }
            mEventListener?.onError(e)
        }
    }

    override fun release() {
        mKernel?.let {
            ilogi { "release" }
            it.stop()
            it.setOnErrorListener(null)
            it.setOnCompletionListener(null)
            it.setOnInfoListener(null)
            it.setOnBufferingUpdateListener(null)
            it.setOnPreparedListener(null)
            it.setOnVideoSizeChangedListener(null)
            it.setDisplay(null)
            it.setSurface(null)
            object : Thread() {
                val temp = it
                override fun run() {
                    try {
                        ilogd { "release ijk kernel on thread." }
                        temp.release()
                    } catch (e: Throwable) {
                        iloge(e) {
                            "release ijk kernel($temp) error on thread."
                        }
                    }
                }
            }.start()
        }
        mKernel = null
    }

    override fun getCurrentPosition(): Long {
        return mKernel?.currentPosition ?: 0
    }

    override fun getDuration(): Long {
        return mKernel?.duration ?: 0
    }

    override fun getBufferedPercentage(): Int {
        return mBufferedPercent
    }

    override fun setSurface(surface: Surface?) {
        requirePlayer().setSurface(surface)
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        requirePlayer().setDisplay(holder)
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        requirePlayer().setVolume(leftVolume, rightVolume)
    }

    override fun setLooping(looping: Boolean) {
        requirePlayer().isLooping = looping
    }

    override fun setSpeed(speed: Float) {
        requirePlayer().setSpeed(speed)
    }

    override fun getSpeed(): Float {
        return mKernel?.getSpeed(1f) ?: 1f
    }

    override fun getTcpSpeed(): Long {
        return mKernel?.tcpSpeed ?: 0
    }

    override fun onError(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
        iloge { "onError(what=$what,extra=$extra)" }
        mEventListener?.onError(PlayerException.create(what, extra))
        return true
    }

    override fun onCompletion(mp: IMediaPlayer) {
        iloge { "onCompletion" }
        mEventListener?.onCompletion()
    }

    override fun onInfo(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
        ilogi { "onInfo(what=$what,extra=$extra)" }
        mEventListener?.onInfo(what, extra)
        return true
    }

    override fun onBufferingUpdate(mp: IMediaPlayer, percent: Int) {
        ilogi { "onBufferingUpdate(percent=$percent)" }
        mBufferedPercent = percent
    }

    override fun onPrepared(mp: IMediaPlayer) {
        iloge { "onPrepared" }
        mEventListener?.onPrepared()
    }

    override fun onVideoSizeChanged(
        mp: IMediaPlayer,
        width: Int,
        height: Int,
        sar_num: Int,
        sar_den: Int
    ) {
        iloge { "onVideoSizeChanged(width=$width,height=$height,sar_num=$sar_num,sar_den=$sar_den)" }
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        if (width == 0 || height == 0) {
            ilogw { "onVideoSizeChanged(width=$width,height=$height,sar_num=$sar_num,sar_den=$sar_den) videoWidth/videoHeight is 0,图形区域可能不可见。" }
        }
        mEventListener?.onVideoSizeChanged(videoWidth, videoHeight)
    }

    override fun onNativeInvoke(what: Int, args: Bundle): Boolean {
        plogi2("IikPlayer") { "onNativeInvoke(what=$what,args=$args)" }
        return true
    }

    /**
     * 是否是视频资源
     */
    private fun isVideoSource(): Boolean {
        return mKernel?.trackInfo?.any { trackInfo ->
            trackInfo.trackType == ITrackInfo.MEDIA_TRACK_TYPE_VIDEO
        } ?: false
    }

    private inline fun requirePlayerBlock(block: (IjkMediaPlayer) -> Unit) {
        block(requirePlayer())
    }

    private fun createKernel(): IjkMediaPlayer {
        ilogi { "create ijk kernel." }
        return IjkMediaPlayer().also {
            it.setOnErrorListener(this)
            it.setOnCompletionListener(this)
            it.setOnInfoListener(this)
            it.setOnBufferingUpdateListener(this)
            it.setOnPreparedListener(this)
            it.setOnVideoSizeChangedListener(this)
            it.setOnNativeInvokeListener(this)
        }
    }

    init {
        //native日志 todo  java.lang.UnsatisfiedLinkError: No implementation found for void tv.danmaku.ijk.media.player.IjkMediaPlayer.native_setLogLevel(int)
//        IjkMediaPlayer.native_setLogLevel(if (isDebuggable) IjkMediaPlayer.IJK_LOG_INFO else IjkMediaPlayer.IJK_LOG_SILENT)
        mKernel = createKernel()
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_WARN)
    }

}