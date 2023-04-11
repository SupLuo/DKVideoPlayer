package droid.unicstar.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import droid.unicstar.videoplayer.controller.UNSRenderControl
import droid.unicstar.videoplayer.controller.UNSWindowModelControl
import droid.unicstar.videoplayer.render.AspectRatioType
import droid.unicstar.videoplayer.render.UNSRender
import droid.unicstar.videoplayer.render.UNSRenderFactory
import droid.unicstar.videoplayer.render.UNSRenderProxy
import xyz.doikki.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.internal.ScreenModeHandler
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播放器显示容器：包含Controller、Render
 * 创建该容器方便在全屏、小窗、等不同窗口之间切换
 * @note 该控件设计上考虑可以使用ApplicationContext来创建使用，达到界面间共享
 */
open class UNSDisplayContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), UNSRenderControl, UNSWindowModelControl {

//    /**
//     * 获取Activity，优先通过Controller去获取Activity
//     */
//    private val mPreferredActivity: Activity? get() = context.getActivityContext()
//
//    private val mActivity: Activity get() = mPreferredActivity!!

    private val mRender: UNSRenderProxy

    /**
     * 控制器
     */
    var videoController: MediaController? = null
        private set

    /**
     * 设置控制器，传null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?) {
        if(mediaController == videoController){
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
     */
    private fun removeController(){
        videoController?.let {//移除之前已添加的控制器
            logd("remove current video controller.")
            removeView(it)
        }
    }


    private val mOnScreenModeChangeListeners =
        CopyOnWriteArrayList<UNSVideoView.OnScreenModeChangeListener>()

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
     * 通知当前界面模式发生了变化
     */
    private fun notifyScreenModeChanged(@UNSVideoView.ScreenMode screenMode: Int) {
        //todo 既然通过通知对外发布了screenmode的改变，是否就不应该再主动
        videoController?.setScreenMode(screenMode)
        mOnScreenModeChangeListeners.forEach {
            it.onScreenModeChanged(screenMode)
        }
    }

    //屏幕模式切换帮助类
    private val mScreenModeHandler: ScreenModeHandler = ScreenModeHandler()

    /**
     * 获取渲染视图的名字
     * @return
     */
    val renderName: String get() = mRender.renderName

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

    /***************Start UNSRenderControl**************************/
    override fun setRenderViewFactory(renderViewFactory: UNSRenderFactory?) {
        mRender.setRenderViewFactory(renderViewFactory)
    }

    override fun setScreenAspectRatioType(@AspectRatioType aspectRatioType: Int) {
        mRender.setAspectRatioType(aspectRatioType)
    }

    override fun screenshot(highQuality: Boolean, callback: UNSRender.ScreenShotCallback) {
        mRender.screenshot(highQuality, callback)
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        mRender.setVideoSize(videoWidth, videoHeight)
    }

    override fun getVideoSize(): IntArray {
        return mRender.videoSize
    }

    override fun setVideoRotation(degree: Int) {
        mRender.setVideoRotation(degree)
    }

    override fun setMirrorRotation(enable: Boolean) {
        mRender.setMirrorRotation(enable)
    }
    /***************End UNSRenderControl**************************/

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

    init {
        mRender = UNSRenderProxy(this)
        mRender.setAspectRatioType(screenAspectRatioType)
    }
}