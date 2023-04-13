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
import droid.unicstar.videoplayer.controller.MediaController
import droid.unicstar.videoplayer.controller.UNSContainerControl
import droid.unicstar.videoplayer.controller.UNSRenderControl
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.render.UNSRender
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.R
import droid.unicstar.videoplayer.widget.ScreenModeHandler
import java.lang.ref.SoftReference
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
) : FrameLayout(context, attrs), UNSRenderControl by mRender, UNSContainerControl {

    //绑定的界面
    private var mBindActivityRef: SoftReference<Activity?>? = null

    //绑定的容器：即正常情况下显示播放器所属的容器
    private var mBindContainer: SoftReference<FrameLayout?>? = null

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

    private var mPlayerRef: SoftReference<UNSPlayerProxy?>? = null

    private val mPlayerEventListener = object : UNSPlayer.EventListener {
        override fun onInfo(what: Int, extra: Int) {
            try {
                when (what) {
                    UNSPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> setVideoRotation(extra)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            setVideoSize(width, height)
        }
    }

    private val mPlayerStateListener = UNSPlayer.OnPlayStateChangeListener { playState ->
        when (playState) {
            UNSPlayer.STATE_PLAYING -> {
                if (!this@UNSDisplayContainer.keepScreenOn)
                    this@UNSDisplayContainer.keepScreenOn = true
            }
            UNSPlayer.STATE_PAUSED, UNSPlayer.STATE_PLAYBACK_COMPLETED, UNSPlayer.STATE_ERROR -> {
                if (this@UNSDisplayContainer.keepScreenOn)
                    this@UNSDisplayContainer.keepScreenOn = false
            }
        }
        videoController?.setPlayerState(playState)
    }

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
     * 绑定Activity
     */
    fun bindActivity(activity: Activity) {
        if(activity == getActivity()){
            logw("[DisplayContainer]bindActivity:the activity is same with current activity.ignore set.")
            return
        }
        //todo 绑定界面之后应该完善状态？
        mBindActivityRef = SoftReference(activity)
    }

    /**
     * 解绑Activity
     */
    fun unbindActivity() {
        //todo 如果是小窗或者全屏，从界面中移除
        mBindActivityRef
    }

    /**
     * 绑定所在的容器：即正常显示所在的容器
     */
    fun bindContainer(container: FrameLayout) {
        //todo 注意考虑从原来的容器中移除、并考虑当前的view状态
    }

    /**
     * 解绑容器
     */
    fun unbindContainer() {
        //解除容器的时候，记得考虑从parent 中移除
    }

    /**
     * 绑定播放器
     */
    fun bindPlayer(player: UNSPlayerProxy) {
        logd("[DisplayContainer]:bindPlayer")
        val currentPlayer = mPlayerRef?.get()
        if (currentPlayer != player) {
            logd("[DisplayContainer]:bindPlayer not same with current player,un ref current player.")
            currentPlayer?.let {
                it.removeEventListener(mPlayerEventListener)
                it.removeOnPlayStateChangeListener(mPlayerStateListener)
            }
            mPlayerRef = SoftReference(player)
            player.addEventListener(mPlayerEventListener)
            player.addOnPlayStateChangeListener(mPlayerStateListener)
        } else {
            logd("[DisplayContainer]:same with current player,ignore ref player listeners.")
        }
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
            logd("controller attached to display container.")
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
    override fun isFullScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_FULL
    }

    /**
     * 当前是否处于小屏状态（视图处于小屏）
     */
    override fun isTinyScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_TINY
    }

    private inline fun getActivity(): Activity? = mBindActivityRef?.get()

    private inline fun requireActivity(methodName: () -> String): Activity {
        return mBindActivityRef?.get()
            ?: throw IllegalArgumentException("must invoke ${::bindActivity.name} method to bind activity before call method ${methodName.invoke()}")
    }

    private inline fun requireContainer(methodName: () -> String): FrameLayout {
        return mBindContainer?.get()
            ?: throw IllegalArgumentException("must invoke ${::bindContainer.name} method to bind activity before call method ${methodName.invoke()}")
    }


    /**
     * 横竖屏切换
     * 如果切换为横屏、并且当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面；
     * 如果切换为竖屏，并且当前界面为界面间共享的播放器，则调用此方法前，需要先调用[bindContainer]绑定竖屏显示状态下的容器；并且也可以调用[bindActivity]指定当前的界面，用于退出全屏时设置Activity为竖屏
     *
     * @return
     */
    override fun toggleFullScreen(): Boolean {
        return if (isFullScreen()) {
            stopFullScreen()
        } else {
            startFullScreen()
        }
    }

    /**
     * 开始全屏
     * 如果当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面；
     */
    override fun startFullScreen(isLandscapeReversed: Boolean): Boolean {
        val activity = requireActivity {
            "startFullScreen"
        }
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
        return startVideoViewFullScreen()
    }

    /**
     * 整个播放视图（Render、Controller）全屏
     * 如果当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面；
     */
    override fun startVideoViewFullScreen(tryHideSystemBar: Boolean): Boolean {
        if (isFullScreen()) return false
        val activity = requireActivity {
            "startVideoViewFullScreen"
        }
        if (mScreenModeHandler.startFullScreen(activity, this, tryHideSystemBar)) {
            screenMode = UNSVideoView.SCREEN_MODE_FULL
            return true
        }
        return false
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun stopFullScreen(): Boolean {
        val activity = getActivity()
        if (activity != null && activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return stopVideoViewFullScreen()
    }

    /**
     * 整个播放视图（Render、Controller）退出全屏（切换为竖屏）;则调用此方法前，需要先调用[bindContainer]绑定竖屏显示的容器；
     */
    override fun stopVideoViewFullScreen(tryShowSystemBar: Boolean): Boolean {
        if (!isFullScreen()) return false
        val container = requireContainer {
            "stopVideoViewFullScreen"
        }
        val activity = getActivity()
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
     * 如果当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面
     */
    override fun startTinyScreen() {
        if (isTinyScreen()) return
        val activity = requireActivity {
            ::startTinyScreen.name
        }
        if (mScreenModeHandler.startTinyScreen(activity, this)) {
            screenMode = UNSVideoView.SCREEN_MODE_TINY
        }
    }

    /**
     * 退出小屏;调用此方法前，需要先调用[bindContainer]绑定界面
     */
    override fun stopTinyScreen() {
        if (!isTinyScreen()) return
        val container: ViewGroup = requireContainer {
            ::stopTinyScreen.name
        }
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


    init {
        //如果当前容器是通过Activity上下文构建的，则默认绑定的界面为该Activity
        val activity = context.getActivityContext()
        if (activity != null) {
            mBindActivityRef = SoftReference(activity)
        }
    }
}