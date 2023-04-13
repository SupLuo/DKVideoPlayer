package droid.unicstar.videoplayer.controller


interface UNSContainerControl {
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
}