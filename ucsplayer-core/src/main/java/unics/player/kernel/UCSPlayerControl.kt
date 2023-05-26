package unics.player.kernel


import unics.player.UCSPlayerManager
import unics.player.widget.AudioFocusHelper
import xyz.doikki.videoplayer.ProgressManager

/**
 * Player控制层提供的功能；具体由[PlayerProxy]实现,即提供了播放器常规拥有的所有功能
 */
interface UCSPlayerControl : UCSPlayerBase {

    /**
     * 默认值[UCSPlayerManager.isPlayerKernelReusable]
     * [UCSPlayer]是否重用（即在播放器贼每次使用时，是否重用已有的Player：以前的版本是每次都会创建一个新的播放器）
     */
    fun setPlayerReusable(reusable: Boolean)

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
     * 自定义播放核心，继承[PlayerFactory]实现自己的播放核心
     * 需要在未开始播放前设置才有效，已处于播放状态时设置工厂将在下一次播放的时候生效
     */
    fun setPlayerFactory(playerFactory: PlayerFactory<out UCSPlayer>)

    /**
     * 是否开启AudioFocus监听，默认[UCSPlayerManager.isAudioFocusEnabled]，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见[AudioFocusHelper]
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean)

    /**
     * 设置进度管理器，用于保存播放进度
     * 默认配置[UCSPlayerManager.progressManager]
     */
    fun setProgressManager(progressManager: ProgressManager?)

    /**
     * 当前播放器状态
     */
    @get:UCSPlayer.PlayState
    val currentState: Int

    /**
     * 是否处于可播放状态
     */
    fun isInPlaybackState():Boolean

    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    fun addOnPlayStateChangeListener(listener: UCSPlayer.OnPlayStateChangeListener)

    /**
     * 移除某个播放状态监听
     */
    fun removeOnPlayStateChangeListener(listener: UCSPlayer.OnPlayStateChangeListener)

    fun addEventListener(eventListener: UCSPlayer.EventListener)

    fun removeEventListener(eventListener: UCSPlayer.EventListener)
}