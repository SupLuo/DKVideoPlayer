package unics.player.controller

import androidx.annotation.ColorInt
import unics.player.ScreenMode
import unics.player.kernel.UCSPlayerControl
import unics.player.render.UCSRenderControl

/**
 * 整个播放器视图所在容器控制层提供的功能；具体由[unics.player.DisplayContainer]实现
 */
interface UCSContainerControl : UCSRenderControl {

    /**
     * 屏幕模式发生变化监听
     */
    fun interface OnScreenModeChangeListener {
        fun onScreenModeChanged(@ScreenMode screenMode: Int)
    }

    /**
     * 绑定播放器,如果为null则会解除之前已绑定的播放器
     */
    fun bindPlayer(player: UCSPlayerControl?)

    /**
     * 设置控制器，null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?)

    /**
     * 释放资源
     */
    fun release()

    @get:ScreenMode
    val screenMode: Int

    /**
     * 添加屏幕变化监听
     */
    fun addOnScreenModeChangeListener(listener: OnScreenModeChangeListener)

    /**
     * 移除屏幕模式变化监听
     */
    fun removeOnScreenModeChangeListener(listener: OnScreenModeChangeListener)

    /**
     * 启用设备角度传感器(用于自动横竖屏切换)，默认[xyz.doikki.videoplayer.DKManager.isOrientationSensorEnabled]
     *
     * @param enable true:开启，默认关闭
     */
    fun setEnableOrientationSensor(enable: Boolean)

    /**
     * 判断是否处于全屏状态（视图处于全屏）
     */
    fun isFullScreen(): Boolean

    /**
     * 当前是否处于小屏状态（视图处于小屏）
     */
    fun isTinyScreen(): Boolean

    /**
     * 横竖屏切换
     */
    fun toggleFullScreen(): Boolean

    /**
     * 开始全屏
     */
    fun startFullScreen(): Boolean = startFullScreen(false)


    fun startFullScreen(isLandscapeReversed: Boolean): Boolean

    /**
     * 停止全屏：切换为竖屏
     */
    fun stopFullScreen(): Boolean

    /**
     * 整个播放视图（Render、Controller）全屏
     */
    fun startVideoViewFullScreen(): Boolean = startVideoViewFullScreen(true)

    fun startVideoViewFullScreen(tryHideSystemBar: Boolean): Boolean

    /**
     * 整个播放视图（Render、Controller）退出全屏
     */
    fun stopVideoViewFullScreen(): Boolean = stopVideoViewFullScreen(true)

    fun stopVideoViewFullScreen(tryShowSystemBar: Boolean): Boolean

    /**
     * 开启小屏
     */
    fun startTinyScreen()

    /**
     * 退出小屏
     */
    fun stopTinyScreen()

    /**
     * 设置是否适配刘海屏
     */
    fun setAdaptCutout(adaptCutout: Boolean)

    /**
     * 是否有刘海屏
     */
    fun hasCutout(): Boolean

    /**
     * 刘海的高度
     */
    fun getCutoutHeight(): Int

    /**
     * 设置容器背景色
     */
    fun setPlayerBackgroundColor(@ColorInt color: Int)

}