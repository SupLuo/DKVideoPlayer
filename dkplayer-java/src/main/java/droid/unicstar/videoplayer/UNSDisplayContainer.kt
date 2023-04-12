package droid.unicstar.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import droid.unicstar.videoplayer.controller.UNSRenderControl
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.render.UNSRender
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.R
import xyz.doikki.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.internal.ScreenModeHandler
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 本类职责：处理器视频显示相关的逻辑，包含Render、窗口模式、Controller三者相关功能；
 * 不包括Player相关的依赖，这样便于视图层的复用和共用，也方便Player的各种灵活形态（共享的、全局的、局部的）都可以。
 * @see bindPlayer 通过该方法绑定对应的播放器
 *
 * Render部分：可使用的功能参考[UNSRenderControl]
 * 窗口模式部分：[isFullScreen]、[isTinyScreen]、[toggleFullScreen]、[startFullScreen]、[stopFullScreen]
 * [startTinyScreen]、[stopTinyScreen]、[startVideoViewFullScreen]、[stopVideoViewFullScreen]
 * 、[addOnScreenModeChangeListener]、[removeOnScreenModeChangeListener]
 * Controller部分：
 *
 * @note todo 该控件设计上考虑可以使用ApplicationContext来创建使用，达到界面间共享（只是理论层面，还未测试）
 */
open class UNSDisplayContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    private val mRender: UNSRenderProxy = UNSRenderProxy()
) : FrameLayout(context, attrs), UNSRenderControl by mRender {

    /**
     * 控制器
     */
    var videoController: MediaController? = null
        private set

    //对外提供的
    val render: UNSRender get() = mRender

    private val mOnScreenModeChangeListeners =
        CopyOnWriteArrayList<UNSVideoView.OnScreenModeChangeListener>()

    //屏幕模式切换帮助类
    private val mScreenModeHandler: ScreenModeHandler = ScreenModeHandler()

    /**
     * 获取渲染视图的名字
     */
    val renderName: String get() = mRender.renderName

    /**
     * 当前屏幕模式：普通、全屏、小窗口
     */
    @UNSVideoView.ScreenMode
    var screenMode: Int = UNSVideoView.SCREEN_MODE_NORMAL
        private set(@UNSVideoView.ScreenMode screenMode) {
            if (field != screenMode) {
                field = screenMode
                notifyScreenModeChanged(screenMode)
            }
        }

    /**
     * 绑定播放器
     */
    fun bindPlayer(player: UNSPlayer) {
        mRender.bindPlayer(player)
//        videoController?.setMediaPlayer(player)
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?) {
        if (mediaController == videoController) {
            logd("same with current video controller,set ignore.")
            return
        }
        removeController()
        videoController = mediaController
        mediaController?.let { controller ->
            controller.removeFromParent()
            //添加控制器
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(controller, params)

            //fix：video view先调用全屏方法后调用setController的情况下，controller的screen mode与video view的模式不一致问题（比如引起手势无效等）
            controller.setScreenMode(screenMode)
            logd("attach controller to display container.")
        }
    }

    /**
     * 移除控制器
     * @return 被移除的控制器
     */
    private fun removeController(): MediaController? {
        val vc = videoController ?: return null
        removeView(vc)
        videoController = null
        return vc
    }

    /**
     * 通知当前界面模式发生了变化
     */
    private fun notifyScreenModeChanged(@UNSVideoView.ScreenMode screenMode: Int) {
        //todo 既然通过通知对外发布了screenmode的改变，是否就不应该再主动
        videoController?.setScreenMode(screenMode)
        mOnScreenModeChangeListeners.forEach {
            it.onScreenModeChanged(screenMode)
        }
    }

    /**
     * 添加屏幕模式变化监听
     */
    fun addOnScreenModeChangeListener(listener: UNSVideoView.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.add(listener)
    }

    /**
     * 移除屏幕模式变化监听
     */
    fun removeOnScreenModeChangeListener(listener: UNSVideoView.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.remove(listener)
    }

    /**
     * 判断是否处于全屏状态（视图处于全屏）
     */
    fun isFullScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_FULL
    }

    /**
     * 当前是否处于小屏状态（视图处于小屏）
     */
    fun isTinyScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_TINY
    }

    /**
     * 横竖屏切换
     *
     * @return
     * @note 由于设计上支持界面间共享播放器，因此需要明确指定[Activity]对象
     */
    fun toggleFullScreen(activity: Activity, container: ViewGroup): Boolean {
        return if (isFullScreen()) {
            stopFullScreen(container, activity)
        } else {
            startFullScreen(activity)
        }
    }

    /**
     * 开始全屏
     */
    @JvmOverloads
    fun startFullScreen(
        activity: Activity,
        isLandscapeReversed: Boolean = false
    ): Boolean {
        if (isLandscapeReversed) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
        } else {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        return startVideoViewFullScreen(activity)
    }

    /**
     * 整个播放视图（Render、Controller）全屏
     * @param activity 在指定界面全屏
     */
    @JvmOverloads
    fun startVideoViewFullScreen(activity: Activity, tryHideSystemBar: Boolean = true): Boolean {
        if (isFullScreen()) return false
        if (mScreenModeHandler.startFullScreen(activity, this, tryHideSystemBar)) {
            screenMode = UNSVideoView.SCREEN_MODE_FULL
            return true
        }
        return false
    }

    /**
     * 停止全屏
     */
    @SuppressLint("SourceLockedOrientationActivity")
    fun stopFullScreen(
        container: ViewGroup,
        activity: Activity? = container.context.getActivityContext()
    ): Boolean {
        if (activity != null && activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return stopVideoViewFullScreen(container)
    }

    /**
     * 整个播放视图（Render、Controller）退出全屏
     * @param container 停止全屏之后，用于包含当前view的容器
     */
    @JvmOverloads
    fun stopVideoViewFullScreen(container: ViewGroup, tryShowSystemBar: Boolean = true): Boolean {
        if (!isFullScreen()) return false
        val activity = container.context.getActivityContext()
        val changed = if (activity != null && tryShowSystemBar) {
            mScreenModeHandler.stopFullScreen(activity, container, this)
        } else {
            mScreenModeHandler.stopFullScreen(container, this)
        }
        if (changed) {
            screenMode = UNSVideoView.SCREEN_MODE_NORMAL
            return true
        }
        return false
    }

    /**
     * 开启小屏
     */
    fun startTinyScreen(activity: Activity) {
        if (isTinyScreen()) return
        if (mScreenModeHandler.startTinyScreen(activity, this)) {
            screenMode = UNSVideoView.SCREEN_MODE_TINY
        }
    }

    /**
     * 退出小屏
     * @param container 停止小屏后，用于包含当前view的容器
     */
    fun stopTinyScreen(container: ViewGroup) {
        if (!isTinyScreen()) return
        if (mScreenModeHandler.stopTinyScreen(container, this)) {
            screenMode = UNSVideoView.SCREEN_MODE_NORMAL
        }
    }

    override fun addFocusables(views: ArrayList<View>, direction: Int) {
        val controller = videoController
        if (controller != null && controller.canTakeFocus) {
            views.add(controller)//controller能够获取焦点的情况下，优先只让controller获取焦点
            return
        }
        super.addFocusables(views, direction)
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return videoController?.onBackPressed().orDefault()
    }

    fun release() {
        mRender.release()
    }

    /**
     * 解析对应参数：一般是从外层容器从解析
     */
    fun applyAttributes(context: Context, attrs: AttributeSet) {
        //读取xml中的配置，并综合全局配置
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DKVideoView)
        //todo这两个参数的处理
        val audioFocus: Boolean =
            ta.getBoolean(R.styleable.DKVideoView_enableAudioFocus, DKManager.isAudioFocusEnabled)
        val looping = ta.getBoolean(R.styleable.DKVideoView_looping, false)

        if (ta.hasValue(R.styleable.DKVideoView_screenScaleType)) {
            val screenAspectRatioType =
                ta.getInt(R.styleable.DKVideoView_screenScaleType, DKManager.screenAspectRatioType)
            setAspectRatioType(screenAspectRatioType)
        }
        if (ta.hasValue(R.styleable.DKVideoView_playerBackgroundColor)) {
            val playerBackgroundColor =
                ta.getColor(R.styleable.DKVideoView_playerBackgroundColor, Color.TRANSPARENT)
            setBackgroundColor(playerBackgroundColor)
        }
        ta.recycle()
    }
}