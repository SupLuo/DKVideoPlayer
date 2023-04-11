package droid.unicstar.videoplayer.player.sys

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import droid.unicstar.videoplayer.logw
import droid.unicstar.videoplayer.orDefault
import droid.unicstar.videoplayer.player.AbstractCSPlayer
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.player.CSPlayerException
import droid.unicstar.videoplayer.tryIgnore
import java.io.IOException

/**
 * 基于系统[android.media.MediaPlayer]封装
 * 注意：不推荐，兼容性差，建议使用IJK或者Exo播放器
 */
class SysDKPlayer : AbstractCSPlayer() {

    //系统播放器核心
    private var mKernel: MediaPlayer? = null

    //当前缓冲百分比
    private var mBufferedPercent = 0//todo 是否需要在播放结束或者错误的时候把该变量设置为0

    /**
     * 是否正在准备阶段：用于解决[android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START]多次回调问题
     */
    private var isPreparing = false

    private val mKernelListener =
        object : MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnVideoSizeChangedListener {

            override fun onPrepared(mp: MediaPlayer) {
                eventListener?.onPrepared()
            }

            override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                eventListener?.onError(
                    CSPlayerException(
                        what,
                        extra
                    )
                )
                return true
            }

            override fun onCompletion(mp: MediaPlayer) {
                eventListener?.onCompletion()
            }

            override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                //解决MEDIA_INFO_VIDEO_RENDERING_START多次回调问题
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        if (isPreparing) {
                            eventListener?.onInfo(what, extra)
                            isPreparing = false
                        }
                    }
                    else -> {
                        eventListener?.onInfo(what, extra)
                    }
                }
                return true
            }

            override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
                mBufferedPercent = percent
            }

            override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
                eventListener?.let {
                    val videoWidth = mp.videoWidth
                    val videoHeight = mp.videoHeight
                    if (videoWidth != 0 && videoHeight != 0) {
                        it.onVideoSizeChanged(videoWidth, videoHeight)
                    }
                }
            }
        }

    override fun init() {
        mKernel = MediaPlayer().also {
            it.setAudioStreamType(AudioManager.STREAM_MUSIC)
            it.setOnErrorListener(mKernelListener)
            it.setOnCompletionListener(mKernelListener)
            it.setOnInfoListener(mKernelListener)
            it.setOnBufferingUpdateListener(mKernelListener)
            it.setOnPreparedListener(mKernelListener)
            it.setOnVideoSizeChangedListener(mKernelListener)
        }
    }

    @Throws(
        IOException::class,
        IllegalArgumentException::class,
        SecurityException::class,
        IllegalStateException::class
    )
    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        try {
            logOnKernelInvalidate()
            //这里转换成了application context，一般没问题
            mKernel!!.setDataSource(context.applicationContext, uri, headers)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        try {
            logOnKernelInvalidate()
            mKernel!!.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun start() {
        try {
            logOnKernelInvalidate()
            mKernel!!.start()
            eventListener?.let {
                // 修复播放纯音频时状态出错问题:：系统播放器播放纯音频没有对应回调
                val isVideo = mKernel!!.trackInfo?.any { trackInfo ->
                    trackInfo.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO
                }.orDefault()
                if (!isVideo) {
                    it.onInfo(UNSPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0)
                }
            }
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun pause() {
        try {
            logOnKernelInvalidate()
            mKernel!!.pause()
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun stop() {
        try {
            logOnKernelInvalidate()
            mKernel!!.stop()
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun prepareAsync() {
        try {
            logOnKernelInvalidate()
            isPreparing = true
            mKernel!!.prepareAsync()
        } catch (e: Throwable) {
            isPreparing = false
            handlePlayerOperationException(e)
        }
    }

    override fun reset() {
        mKernel?.let {
            it.stop()
            it.reset()
            it.setSurface(null)
            it.setDisplay(null)
            it.setVolume(1f, 1f)
        }
    }

    override fun isPlaying(): Boolean {
        logOnKernelInvalidate()
        return mKernel?.isPlaying.orDefault()
    }

    override fun seekTo(msec: Long) {
        try {
            logOnKernelInvalidate()
            if (Build.VERSION.SDK_INT >= 26) {
                //使用这个api seekTo定位更加准确 支持android 8.0以上的设备 https://developer.android.com/reference/android/media/MediaPlayer#SEEK_CLOSEST
                mKernel!!.seekTo(msec, MediaPlayer.SEEK_CLOSEST)
            } else {
                mKernel!!.seekTo(msec.toInt())
            }
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun release() {
        mKernel?.let {
            it.setOnErrorListener(null)
            it.setOnCompletionListener(null)
            it.setOnInfoListener(null)
            it.setOnBufferingUpdateListener(null)
            it.setOnPreparedListener(null)
            it.setOnVideoSizeChangedListener(null)
            it.stop()

            object : Thread() {
                val temp = it
                override fun run() {
                    try {
                        temp.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
        mKernel = null
    }

    override fun getCurrentPosition(): Long {
        logOnKernelInvalidate()
        return mKernel?.currentPosition.orDefault().toLong()
    }

    override fun getDuration(): Long {
        logOnKernelInvalidate()
        return mKernel?.duration.orDefault().toLong()
    }

    override fun getBufferedPercentage(): Int {
        return mBufferedPercent
    }

    override fun setSurface(surface: Surface?) {
        try {
            logOnKernelInvalidate()
            mKernel!!.setSurface(surface)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        try {
            logOnKernelInvalidate()
            mKernel!!.setDisplay(holder)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        logOnKernelInvalidate()
        mKernel?.setVolume(leftVolume, rightVolume)
    }

    override fun setLooping(isLooping: Boolean) {
        logOnKernelInvalidate()
        mKernel?.isLooping = isLooping
    }

    override fun setSpeed(speed: Float) {
        // only support above Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            logw("SysDKPlayer", "Android MediaPlayer do not support set speed")
            return
        }
        logOnKernelInvalidate()
        mKernel?.let {
            it.playbackParams = it.playbackParams.setSpeed(speed)
        }
    }

    override fun getSpeed(): Float {
        tryIgnore {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                logOnKernelInvalidate()
                return mKernel?.playbackParams?.speed.orDefault(1f)
            } else {
                logw("SysDKPlayer", "Android MediaPlayer do not support tcp speed")
            }
        }
        return 1f
    }

    private fun logOnKernelInvalidate() {
        tryIgnore {
            if (mKernel == null)
                logw(
                    "SysDKPlayer",
                    "player is null,can not invoke ${Thread.currentThread().stackTrace[2].methodName} method，please call init first."
                )
        }
    }

    private fun handlePlayerOperationException(e: Throwable) {
        e.printStackTrace()
        eventListener?.onError(e)
    }

}