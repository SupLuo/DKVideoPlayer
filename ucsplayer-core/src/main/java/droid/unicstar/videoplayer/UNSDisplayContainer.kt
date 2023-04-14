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
import droid.unicstar.videoplayer.widget.DeviceOrientationSensorHelper
import droid.unicstar.videoplayer.widget.ScreenModeHandler
import xyz.doikki.videoplayer.DKManager
import xyz.doikki.videoplayer.R
import xyz.doikki.videoplayer.util.CutoutUtil
import xyz.doikki.videoplayer.util.L
import xyz.doikki.videoplayer.util.PlayerUtils
import java.lang.ref.SoftReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 本类职责：处理器视频显示相关的逻辑，包含Render、窗口模式、Controller、以及传感器自动旋转方向、刘海屏显示适配等
 * （全部与播放器显示视图相关的逻辑，具体在window显示还是在activity内显示就不限定，播放器是游离的还是内嵌的也不限定）；
 *
 * 不包括Player相关的依赖，这样便于视图层的复用和共用，也方便Player的各种灵活形态（共享的、全局的、局部的）都可以。
 * @see bindPlayer 通过该方法绑定对应的播放器
 *
 * Render部分：可使用的功能参考[UNSRenderControl]定义的方法
 * 窗口模式部分：可使用功能参考[UNSContainerControl]定义定义的方法
 * Controller部分：
 *
 * @note todo 该控件设计上考虑可以使用ApplicationContext来创建使用，达到界面间共享（只是理论层面，还未测试）
 */
open class UNSDisplayContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    private val mRender: UNSRenderProxy = UNSRenderProxy()
) : FrameLayout(context, attrs),UNSContainerControl, UNSRenderControl by mRender {

    //绑定的界面
    private var mBindActivityRef: SoftReference<Activity?>? = null

    //绑定的容器：即正常情况下显示播放器所属的容器
    private var mBindContainerRef: SoftReference<FrameLayout?>? = null

    /**
     * 控制器
     */
    var videoController: MediaController? = null
        private set

    //对外提供的
    val render: UNSRender get() = mRender

    private val mOnScreenModeChangeListeners =
        CopyOnWriteArrayList<UNSContainerControl.OnScreenModeChangeListener>()

    //屏幕模式切换帮助类
    private val mScreenModeHandler: ScreenModeHandler = ScreenModeHandler()

    private var mPlayerRef: SoftReference<UNSPlayerProxy?>? = null

    //是否开启根据传感器获得的屏幕方向进入/退出全屏
    private var mEnableOrientationSensor = DKManager.isOrientationSensorEnabled

    //用户设置是否适配刘海屏
    private var mAdaptCutout = DKManager.isAdaptCutout

    //是否有刘海
    private var mHasCutout: Boolean? = null

    //刘海的高度
    private var mCutoutHeight = 0

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

    private val mDeviceOrientationChangedListener =
        object : DeviceOrientationSensorHelper.DeviceOrientationChangedListener {

            override fun onDeviceDirectionChanged(@DeviceOrientationSensorHelper.DeviceDirection direction: Int) {
                when (direction) {
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_PORTRAIT -> {
                        //切换为竖屏
                        //todo lock lock的情况下不应该进行屏幕旋转
//                        //屏幕锁定的情况
//                        if (mLocked) return
                        //没有开启设备方向监听的情况
                        if (!mEnableOrientationSensor) return
                        stopFullScreen()
                    }
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_LANDSCAPE -> {
                        startFullScreen()
                    }
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_LANDSCAPE_REVERSED -> {
                        startFullScreen(true)
                    }
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_UNKNOWN -> {
                    }
                }
            }
        }

    //屏幕角度传感器监听
    private val mOrientationSensorHelper: DeviceOrientationSensorHelper =
        DeviceOrientationSensorHelper(context.applicationContext, null).also {
            //开始监听设备方向
            it.setDeviceOrientationChangedListener(mDeviceOrientationChangedListener)
        }

    private val mPlayerStateListener = UNSPlayer.OnPlayStateChangeListener { playState ->
        when (playState) {
            UNSPlayer.STATE_IDLE -> {
                mOrientationSensorHelper.disable()
            }
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
    @ScreenMode
    override var screenMode: Int = UNSVideoView.SCREEN_MODE_NORMAL
        internal set(@ScreenMode screenMode) {
            if (field != screenMode) {
                field = screenMode
                notifyScreenModeChanged(screenMode)
            }
        }

    /**
     * 绑定Activity
     */
    fun bindActivity(activity: Activity) {
        if (activity == getActivity()) {
            logw("[DisplayContainer]bindActivity:the activity is same with current activity.ignore set.")
            return
        }
        //todo 绑定界面之后应该完善状态？
        mBindActivityRef = SoftReference(activity)
        mOrientationSensorHelper.attachActivity(activity)
    }

    /**
     * 解绑Activity
     */
    fun unbindActivity() {
        //todo 如果是小窗或者全屏，从界面中移除
        mBindActivityRef = null
        mOrientationSensorHelper.detachActivity()
    }

    /**
     * 绑定所在的容器：即正常显示所在的容器
     */
    fun bindContainer(container: FrameLayout) {
        mBindContainerRef = SoftReference(container)
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
    override fun bindPlayer(player: UNSPlayerProxy) {
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
    private fun notifyScreenModeChanged(@ScreenMode screenMode: Int) {
        //todo 既然通过通知对外发布了screenmode的改变，是否就不应该再主动
        videoController?.setScreenMode(screenMode)
        mOnScreenModeChangeListeners.forEach {
            it.onScreenModeChanged(screenMode)
        }
        setupOrientationSensorAndCutoutOnScreenModeChanged(screenMode)
    }

    /**
     * 在屏幕模式改变了的情况下，调整传感器和刘海屏
     *
     * @param screenMode
     */
    private fun setupOrientationSensorAndCutoutOnScreenModeChanged(@ScreenMode screenMode: Int) {
        //修改传感器
        when (screenMode) {
            UNSVideoView.SCREEN_MODE_NORMAL -> {
                if (mEnableOrientationSensor) {
                    mOrientationSensorHelper.enable()
                } else {
                    mOrientationSensorHelper.disable()
                }
                if (hasCutout()) {
                    CutoutUtil.adaptCutout(context, false)
                }
            }
            UNSVideoView.SCREEN_MODE_FULL -> {
                //在全屏时强制监听设备方向
                mOrientationSensorHelper.enable()
                if (hasCutout()) {
                    CutoutUtil.adaptCutout(context, true)
                }
            }
            UNSVideoView.SCREEN_MODE_TINY -> mOrientationSensorHelper.disable()
        }
    }

    /**
     * 设置是否适配刘海屏
     */
    override fun setAdaptCutout(adaptCutout: Boolean) {
        mAdaptCutout = adaptCutout
    }

    /**
     * 是否有刘海屏
     */
    override fun hasCutout(): Boolean {
        return mHasCutout.orDefault()
    }

    /**
     * 刘海的高度
     */
    override fun getCutoutHeight(): Int {
        return mCutoutHeight
    }

    /**
     * 添加屏幕模式变化监听
     */
    override fun addOnScreenModeChangeListener(listener: UNSContainerControl.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.add(listener)
    }

    /**
     * 移除屏幕模式变化监听
     */
    override fun removeOnScreenModeChangeListener(listener: UNSContainerControl.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.remove(listener)
    }

    override fun setEnableOrientationSensor(enable: Boolean) {
        mEnableOrientationSensor = enable
    }

    override fun isFullScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_FULL
    }

    override fun isTinyScreen(): Boolean {
        return screenMode == UNSVideoView.SCREEN_MODE_TINY
    }

    private inline fun getActivity(): Activity? = mBindActivityRef?.get()

    private inline fun requireActivity(methodName: () -> String): Activity {
        return mBindActivityRef?.get()
            ?: throw IllegalArgumentException("must invoke ${::bindActivity.name} method to bind activity before call method ${methodName.invoke()}")
    }

    private inline fun requireContainer(methodName: () -> String): FrameLayout {
        return mBindContainerRef?.get()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkCutout()
    }

    /**
     * 检查是否需要适配刘海
     */
    private fun checkCutout() {
        if (!mAdaptCutout || mCutoutHeight > 0) {
            logd("[DisplayContainer]checkCutout: adaptCutout = $mAdaptCutout cutoutHeight=$mCutoutHeight")
            return
        }

        val activity = getActivity()
        if (activity != null && mHasCutout == null) {
            mHasCutout = CutoutUtil.allowDisplayToCutout(activity)
            if (mHasCutout.orDefault()) {
                //竖屏下的状态栏高度可认为是刘海的高度
                mCutoutHeight = PlayerUtils.getStatusBarHeightPortrait(context).toInt()
            }
        }
        L.d("hasCutout: $mHasCutout cutout height: $mCutoutHeight")
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        val player = mPlayerRef?.get() ?: return
        if (player.isPlaying() && (mEnableOrientationSensor || isFullScreen())) {
            if (hasWindowFocus) {
                postDelayed({ mOrientationSensorHelper.enable() }, 800)
            } else {
                mOrientationSensorHelper.disable()
            }
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return videoController?.onBackPressed().orDefault()
    }

    override fun release() {
        //关闭屏幕常亮
        keepScreenOn = false
        mRender.release()
        mOrientationSensorHelper.disable()
        mOrientationSensorHelper.setDeviceOrientationChangedListener(null)
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

    override fun onFinishInflate() {
        super.onFinishInflate()
        //布局中加载完成的时候，判断下是否需要自动绑定Container
        try {
            val parent = this.parent
            if(mBindContainerRef?.get() == null && parent is FrameLayout){
                mBindContainerRef = SoftReference(parent)
            }
        }catch (e:Throwable){
            e.printStackTrace()
        }
    }

    init {
        //如果当前容器是通过Activity上下文构建的，则默认绑定的界面为该Activity
        val activity = context.getActivityContext()
        if (activity != null) {
            bindActivity(activity)
        }
        mRender.bindContainer(this)
    }
}