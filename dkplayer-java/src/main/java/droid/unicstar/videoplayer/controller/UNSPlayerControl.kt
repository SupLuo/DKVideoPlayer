package droid.unicstar.videoplayer.controller


import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import droid.unicstar.videoplayer.PartialFunc
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.player.UNSPlayerFactory
import droid.unicstar.videoplayer.widget.AudioFocusHelper
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.ProgressManager


/**
 * Player控制层提供的功能；具体由[droid.unicstar.videoplayer.UNSPlayerProxy]实现
 */
interface UNSPlayerControl {

    /**
     * 开始播放
     */
    fun start()

    /**
     * 暂停
     */
    fun pause()

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

    fun setVolume(
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float
    )

    /*以下是扩展的播放器功能代码*/

    /**
     * 重新播放
     *
     * @param resetPosition 是否重置播放位置；通常有以下情况不用应该不重置播放位置：1、播放失败之后重新播放 2、清晰度切换之后重新播放
     */
    fun replay(resetPosition: Boolean)

    /**
     * 继续播放
     */
    fun resume()

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean)

    /**
     * 设置静音
     *
     * @param isMute true:静音 false：相反
     */
    fun setMute(isMute: Boolean)

    /**
     * 当前是否静音
     *
     * @return true:静音 false：相反
     */
    fun isMute(): Boolean

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
     * 自定义播放核心，继承[UNSPlayerFactory]实现自己的播放核心
     * 需要在未开始播放前设置才有效，已处于播放状态时设置工厂将在下一次播放的时候生效
     */
    fun setPlayerFactory(playerFactory: UNSPlayerFactory<out UNSPlayer>)

    /**
     * 是否开启AudioFocus监听，默认[DKManager.isAudioFocusEnabled]，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean)

    /**
     * 设置进度管理器，用于保存播放进度
     * 默认配置[DKManager.progressManager]
     */
    fun setProgressManager(progressManager: ProgressManager?)

    /**
     * 当前播放器状态
     */
    val currentState: Int

    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    fun addOnPlayStateChangeListener(listener: UNSPlayer.OnPlayStateChangeListener)

    /**
     * 移除某个播放状态监听
     */
    fun removeOnPlayStateChangeListener(listener: UNSPlayer.OnPlayStateChangeListener)
}