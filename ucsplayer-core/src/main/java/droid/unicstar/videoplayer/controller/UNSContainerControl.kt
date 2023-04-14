package droid.unicstar.videoplayer.controller

import droid.unicstar.videoplayer.ScreenMode
import droid.unicstar.videoplayer.UNSPlayerProxy

/**
 * 整个播放器视图所在容器控制层提供的功能；具体由[droid.unicstar.videoplayer.UNSDisplayContainer]实现
 */
interface UNSContainerControl : UNSRenderControl {

    companion object {

        /**
         * 普通模式
         */
        const val SCREEN_MODE_NORMAL = 10

        /**
         * 全屏模式
         */
        const val SCREEN_MODE_FULL = 11

        /**
         * 小窗模式
         */
        const val SCREEN_MODE_TINY = 22
    }

    /**
     * 屏幕模式发生变化监听
     */
    fun interface OnScreenModeChangeListener {
        fun onScreenModeChanged(@ScreenMode screenMode: Int)
    }

    /**
     * 绑定播放器
     */
    fun bindPlayer(player: UNSPlayerProxy)

    /**
     * 释放资源
     */
    fun release()

    @ScreenMode
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
}