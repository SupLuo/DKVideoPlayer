package unics.player.kernel

import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import unics.player.internal.PartialFunc

/**
 * [UCSPlayer] $ [UCSPlayerControl] 共同的部分，通常用于连接各个部分需要播放器相关的行为
 */
interface UCSPlayerBase {

    /**
     * 开始播放
     */
    fun start()

    /**
     * 暂停
     */
    fun pause()

    /**
     * 停止
     * 即让暂停或者正在进行的播放停止，接下来想继续播放的话，得重新设置数据源并调用prepareAsync
     */
    fun stop()

    /**
     * 播放时长
     *
     * @return
     * @note 毫秒
     */
    fun getDuration(): Long

    /**
     * 当前播放位置
     *
     * @return
     * @note 毫秒
     */
    fun getCurrentPosition(): Long

    /**
     * 是否正在播放
     *
     * @return
     */
    fun isPlaying(): Boolean

    /**
     * 调整播放位置
     *
     * @param msec the offset in milliseconds from the start to seek to;偏移位置（毫秒）
     */
    fun seekTo(msec: Long)

    /**
     * 获取缓冲百分比
     */
    @IntRange(from = 0, to = 100)
    fun getBufferedPercentage(): Int

    /**
     * 设置声道音量
     */
    fun setVolume(
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
    )

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean)

    /**
     * 获取播放速度 0.5f：表示0.5倍数 2f:表示2倍速
     * 注意：使用系统播放器时，只有6.0及以上系统才支持，6.0以下默认返回1
     */
    @PartialFunc(message = "使用系统播放器时，只有6.0及以上系统才支持")
    fun getSpeed(): Float

    /**
     * 设置播放速度 0.5f：表示0.5倍数 2f:表示2倍速
     * 注意：使用系统播放器时，只有6.0及以上系统才支持
     */
    @PartialFunc(message = "使用系统播放器时，只有6.0及以上系统才支持")
    fun setSpeed(speed: Float)

    /**
     * 获获取缓冲网速：只有IJK播放器支持
     */
    fun getTcpSpeed(): Long

    /**
     * 设置渲染视频的View,主要用于TextureView
     */
    fun setSurface(surface: Surface?)

    /**
     * 设置渲染视频的View,主要用于SurfaceView
     */
    fun setDisplay(holder: SurfaceHolder?)
}