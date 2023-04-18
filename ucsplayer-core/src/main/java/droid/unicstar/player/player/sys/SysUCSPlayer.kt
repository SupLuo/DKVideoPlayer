package droid.unicstar.player.player.sys

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import droid.unicstar.player.logw
import droid.unicstar.player.orDefault
import droid.unicstar.player.player.BaseUCSPlayer
import droid.unicstar.player.player.UCSPlayerException
import droid.unicstar.player.player.UCSPlayer
import droid.unicstar.player.tryIgnore
import java.io.IOException

/**
 * 基于系统[android.media.MediaPlayer]封装
 * 注意：不推荐，兼容性差，建议使用IJK或者Exo播放器
 */
class SysUCSPlayer : BaseUCSPlayer() {

    //当前缓冲百分比
    private var mBufferedPercent = 0//todo 是否需要在播放结束或者错误的时候把该变量设置为0
    //是否正在准备阶段：用于解决[android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START]多次回调问题
    private var mPreparing = false
    //系统播放器监听
    private val mKernelListener =
        object : MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnVideoSizeChangedListener {

            override fun onPrepared(mp: MediaPlayer) {
                mEventListener?.onPrepared()
            }

            override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                mEventListener?.onError(
                    UCSPlayerException(
                        what,
                        extra
                    )
                )
                return true
            }

            override fun onCompletion(mp: MediaPlayer) {
                mEventListener?.onCompletion()
            }

            override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
                //解决MEDIA_INFO_VIDEO_RENDERING_START多次回调问题
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        if (mPreparing) {
                            mEventListener?.onInfo(what, extra)
                            mPreparing = false
                        }
                    }
                    else -> {
                        mEventListener?.onInfo(what, extra)
                    }
                }
                return true
            }

            override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
                mBufferedPercent = percent
            }

            override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
                mEventListener?.let {
                    val videoWidth = mp.videoWidth
                    val videoHeight = mp.videoHeight
                    if (videoWidth != 0 && videoHeight != 0) {
                        it.onVideoSizeChanged(videoWidth, videoHeight)
                    }
                }
            }
        }

    //系统播放器核心
    private val mKernel: MediaPlayer = createMediaPlayer()

    private var mReleased:Boolean = false

    @Throws(
        IOException::class,
        IllegalArgumentException::class,
        SecurityException::class,
        IllegalStateException::class
    )
    override fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        try {
            checkKernelValidation()
            //这里转换成了application context，一般没问题
            mKernel.setDataSource(context.applicationContext, uri, headers)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        try {
            checkKernelValidation()
            mKernel.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun start() {
        try {
            checkKernelValidation()
            mKernel.start()
            mEventListener?.let {
                // 修复播放纯音频时状态出错问题:：系统播放器播放纯音频没有对应回调
                val isVideo = mKernel!!.trackInfo?.any { trackInfo ->
                    trackInfo.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO
                }.orDefault()
                if (!isVideo) {
                    it.onInfo(UCSPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0)
                }
            }
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun pause() {
        try {
            checkKernelValidation()
            mKernel!!.pause()
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun stop() {
        try {
            checkKernelValidation()
            mKernel!!.stop()
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun prepareAsync() {
        try {
            checkKernelValidation()
            mPreparing = true
            mKernel!!.prepareAsync()
        } catch (e: Throwable) {
            mPreparing = false
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
        checkKernelValidation()
        return mKernel?.isPlaying.orDefault()
    }

    override fun seekTo(msec: Long) {
        try {
            checkKernelValidation()
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
        if(mReleased)
            return
        mReleased = true
        mKernel.let {
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
    }

    override fun getCurrentPosition(): Long {
        checkKernelValidation()
        return mKernel?.currentPosition.orDefault().toLong()
    }

    override fun getDuration(): Long {
        checkKernelValidation()
        return mKernel?.duration.orDefault().toLong()
    }

    override fun getBufferedPercentage(): Int {
        return mBufferedPercent
    }

    override fun setSurface(surface: Surface?) {
        try {
            checkKernelValidation()
            mKernel!!.setSurface(surface)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        try {
            checkKernelValidation()
            mKernel!!.setDisplay(holder)
        } catch (e: Throwable) {
            handlePlayerOperationException(e)
        }
    }

    override fun getTcpSpeed(): Long {
        return 0
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        checkKernelValidation()
        mKernel?.setVolume(leftVolume, rightVolume)
    }

    override fun setLooping(isLooping: Boolean) {
        checkKernelValidation()
        mKernel?.isLooping = isLooping
    }

    override fun setSpeed(speed: Float) {
        // only support above Android M
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            logw("SysDKPlayer", "Android MediaPlayer do not support set speed")
            return
        }
        checkKernelValidation()
        mKernel?.let {
            it.playbackParams = it.playbackParams.setSpeed(speed)
        }
    }

    override fun getSpeed(): Float {
        tryIgnore {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkKernelValidation()
                return mKernel?.playbackParams?.speed.orDefault(1f)
            } else {
                logw("SysDKPlayer", "Android MediaPlayer do not support tcp speed")
            }
        }
        return 1f
    }

    private fun checkKernelValidation() {
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
        mEventListener?.onError(e)
    }

    protected fun createMediaPlayer(): MediaPlayer {
        return MediaPlayer().also {
            it.setAudioStreamType(AudioManager.STREAM_MUSIC)
            it.setOnErrorListener(mKernelListener)
            it.setOnCompletionListener(mKernelListener)
            it.setOnInfoListener(mKernelListener)
            it.setOnBufferingUpdateListener(mKernelListener)
            it.setOnPreparedListener(mKernelListener)
            it.setOnVideoSizeChangedListener(mKernelListener)
        }
    }

}