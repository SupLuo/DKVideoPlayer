package unics.player.kernel.sys

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import unics.player.UCSPManager
import unics.player.internal.*
import unics.player.kernel.BasePlayer
import unics.player.kernel.UCSPlayer

/**
 * 基于系统[android.media.MediaPlayer]封装
 * MediaPlayer学习文档：参考官网 https://developer.android.google.cn/reference/android/media/MediaPlayer
 *
 * 关于系统播放器需要了解播放器状态，参考官方图片：https://developer.android.google.cn/images/mediaplayer_state_diagram.gif
 *
 * @note 本类设计：
 * 本类的设计的目的是提供基于[android.media.MediaPlayer]的[UCSPlayer]实现，并消除系统播放器在各SDK版本间的差异
 * 实现时尽量满足系统播放器的使用规则，避免产生太大差异化的实现
 * 优化了系统播放器release的逻辑
 *
 * @note 在电视/盒子开发，不建议使用系统播放器，兼容性差，建议使用IJK或者Exo播放器
 */
class SysPlayer : BasePlayer() {

    //log的前缀字符串
    private val mLogPrefix: String = "[k-SysPlayer@${this.hashCode()}]"

    //系统播放器
    private var mKernel: MediaPlayer?

    //当前缓冲百分比:需要在开始播放前将其设置为0（目前就prepareAsync）
    private var mBufferedPercent = 0

    fun requirePlayer(): MediaPlayer {
        plogv2(mLogPrefix) { "requirePlayer." }
        return mKernel ?: createKernel().also {
            mKernel = it
        }
    }

    fun getPlayer(): MediaPlayer? = mKernel

    //系统播放器监听
    private val mKernelListener = object : MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnVideoSizeChangedListener {

        override fun onPrepared(mp: MediaPlayer) {
            plogi2(mLogPrefix) { "onPrepared" }
            mEventListener?.onPrepared()
        }

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            plogi2(mLogPrefix) { "onInfo(what=$what, extra=$extra)" }
            mEventListener?.onInfo(what, extra)
            return true
        }

        override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
            plogi2(mLogPrefix) { "onBufferingUpdate(percent=$percent)" }
            mBufferedPercent = percent
        }

        override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
            plogi2(mLogPrefix) { "onVideoSizeChanged(width=$width, height=$height)" }
            mEventListener?.onVideoSizeChanged(width, height)
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            plogi2(mLogPrefix) { "onError(what=$what, extra=$extra)" }
            mEventListener?.onError(newPlayerException(what, extra))
            //返回true，则不会再回调onCompletion
            return true
        }

        override fun onCompletion(mp: MediaPlayer) {
            plogi2(mLogPrefix) { "onCompletion" }
            mEventListener?.onCompletion()
        }
    }

    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        try {
            plogi2(mLogPrefix) { "setDataSource(uri=$uri, headers=$headers)" }
            //这里转换成了application context，一般没问题
            requirePlayer().setDataSource(context.applicationContext, uri, headers)
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "setDataSource(uri=$uri,headers=$headers) error."
            }
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        try {
            plogi2(mLogPrefix) { "setDataSource(AssetFileDescriptor)" }
            requirePlayer().setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "setDataSource(AssetFileDescriptor) error."
            }
        }
    }

    override fun start() {
        try {
            plogi2(mLogPrefix) { "start" }
            requirePlayer().let {
                it.start()
                if (!isVideoSource()) {
                    ploge2(mLogPrefix) { "start -> 开始播放回调兼容：如果不是视频资源，则回调MEDIA_INFO_RENDERING_PREPARED" }
                    mEventListener?.onInfo(UCSPlayer.MEDIA_INFO_RENDERING_PREPARED, 0)
                }
            }
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "start error."
            }
        }
    }

    override fun pause() {
        try {
            plogi2(mLogPrefix) { "pause" }
            mKernel?.pause()
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "pause error"
            }
        }
    }

    override fun stop() {
        try {
            plogi2(mLogPrefix) { "stop" }
            mKernel?.stop()
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "stop error"
            }
        }
    }

    override fun prepareAsync() {
        try {
            plogi2(mLogPrefix) { "prepareAsync" }
            mBufferedPercent = 0
            requirePlayer().prepareAsync()
        } catch (e: Throwable) {
            handlePlayerOperationException(e) { "prepareAsync" }
        }
    }

    override fun reset() {
        try {
            plogi2(mLogPrefix) { "reset" }
            mBufferedPercent = 0
            //感觉直接调用播放器的reset即可，不要做过多额外操作，因为rest之后本身是可以重新设置DataSource + prepare 重用播放器的
            mKernel?.reset()
        } catch (e: Throwable) {
            handlePlayerOperationException(e) { "reset error." }
        }
    }

    override fun isPlaying(): Boolean {
        return mKernel?.isPlaying ?: false
    }

    @SuppressLint("NewApi")
    override fun seekTo(msec: Long) {
        try {
            plogi2(mLogPrefix) { "seekTo($msec)" }
            requirePlayerBlock { player ->
                if (sdkInt >= 26) {
                    //使用这个api seekTo定位更加准确 支持android 8.0以上的设备 https://developer.android.com/reference/android/media/MediaPlayer#SEEK_CLOSEST
                    player.seekTo(msec, MediaPlayer.SEEK_CLOSEST)
                } else {
                    player.seekTo(msec.toInt())
                }
            }
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "seekTo($msec) error"
            }
        }
    }

    override fun release() {
        mKernel?.let {
            plogi2(mLogPrefix) { "release($it)" }
            releasePlayer(it)
        }
        mKernel = null
    }

    override fun getCurrentPosition(): Long {
        return mKernel?.currentPosition?.toLong() ?: 0
    }

    override fun getDuration(): Long {
        return mKernel?.duration?.toLong() ?: 0
    }

    override fun getBufferedPercentage(): Int {
        return mBufferedPercent
    }

    override fun setSurface(surface: Surface?) {
        try {
            plogi2(mLogPrefix) { "setSurface($surface)" }
            requirePlayer().setSurface(surface)
        } catch (e: Throwable) {
            handlePlayerOperationException(e) {
                "setSurface($surface) error"
            }
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        try {
            plogi2(mLogPrefix) { "setDisplay($holder)" }
            requirePlayer().setDisplay(holder)
        } catch (e: Throwable) {
            handlePlayerOperationException(e) { "setDisplay($holder) error" }
        }
    }

    override fun getTcpSpeed(): Long {
        return 0
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        plogi2(mLogPrefix) { "setVolume(leftVolume=$leftVolume,rightVolume=$rightVolume)" }
        requirePlayer().setVolume(leftVolume, rightVolume)
    }

    override fun setLooping(looping: Boolean) {
        plogi2(mLogPrefix) { "setLooping($looping)" }
        requirePlayer().isLooping = looping
    }

    @SuppressLint("NewApi")
    override fun setSpeed(speed: Float) {
        // only support above Android M
        if (sdkInt < Build.VERSION_CODES.M) {
            plogw2(mLogPrefix) { "setSpeed($speed) -> Android MediaPlayer do not support set speed" }
            return
        }
        requirePlayerBlock { player ->
            plogi2(mLogPrefix) { "setSpeed($speed)" }
            player.playbackParams = player.playbackParams.setSpeed(speed)
        }
    }

    @SuppressLint("NewApi")
    override fun getSpeed(): Float {
        return mKernel?.let { player ->
            //密集型方法，sdk int采用变量
            if (sdkInt >= Build.VERSION_CODES.M) {
                player.playbackParams.speed.also {
                    plogi2(mLogPrefix) { "getSpeed = $it" }
                }
            } else {
                1f
            }
        } ?: 1f
    }

    /**
     * 是否是视频资源
     */
    private fun isVideoSource(): Boolean {
        return mKernel?.trackInfo?.any { trackInfo ->
            trackInfo.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO
        } ?: false
    }

    private inline fun handlePlayerOperationException(e: Throwable, creator: () -> String) {
        ploge2(mLogPrefix, e, creator)
        mEventListener?.onError(e)
    }

    private inline fun requirePlayerBlock(block: (MediaPlayer) -> Unit) {
        block(requirePlayer())
    }

    private fun createKernel(): MediaPlayer {
        return UCSPManager.createMediaPlayer().also {
            it.setAudioStreamType(AudioManager.STREAM_MUSIC)
            it.setOnErrorListener(mKernelListener)
            it.setOnCompletionListener(mKernelListener)
            it.setOnInfoListener(mKernelListener)
            it.setOnBufferingUpdateListener(mKernelListener)
            it.setOnPreparedListener(mKernelListener)
            it.setOnVideoSizeChangedListener(mKernelListener)
            plogi2(mLogPrefix) { "system media player created $it" }
        }
    }

    init {
        plogi2(mLogPrefix) { "create system media player kernel." }
        mKernel = createKernel()
    }

}