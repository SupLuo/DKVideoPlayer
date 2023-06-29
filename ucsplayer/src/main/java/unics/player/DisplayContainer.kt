package unics.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import unics.player.controller.MediaController
import unics.player.controller.UCSContainerControl
import unics.player.internal.*
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerControl
import unics.player.render.RenderProxy
import unics.player.render.UCSRender
import unics.player.render.UCSRenderControl
import unics.player.widget.DeviceOrientationSensorHelper
import unics.player.widget.ScreenModeHandler
import xyz.doikki.videoplayer.R
import java.lang.ref.SoftReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 本类职责：处理器视频显示相关的逻辑，包含Render、窗口模式、Controller、以及传感器自动旋转方向、刘海屏显示适配等
 * （全部与播放器显示视图相关的逻辑，具体在window显示还是在activity内显示就不限定，播放器是游离的还是内嵌的也不限定）；
 *
 * 不包括Player相关的依赖，这样便于视图层的复用和共用，也方便Player的各种灵活形态（共享的、全局的、局部的）都可以。
 *
 * Render部分：可使用的功能参考[UCSRenderControl]定义的方法
 * 窗口模式部分：可使用功能参考[UCSContainerControl]定义定义的方法
 * Controller部分：
 *
 * @note todo 该控件设计上考虑可以使用ApplicationContext来创建使用，达到界面间共享（只是理论层面，还未测试）
 *
 *
 * 使用的时候注意以下三个方法
 * @see bindPlayer 通过该方法绑定对应的播放器：即当前视图对应的当前播放器
 * @see bindContainer 指定正常状态所属的容器：即本视图正常显示时，所属的parent viewgroup，也就是播放器常规小窗口显示时所属的容器，用于横竖屏切换的时候切换容器（本类会自动查找容器，即在加载完成之后，如果当前view的parent不为空，并且是framelayout，则将其作为container）
 * @see bindActivity 指定绑定所属的activity，因为全屏、浮窗播放等操作需要当前Activity；本类会在创建的时候，尝试从Context中获取对应的Activity
 */
open class DisplayContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    private val mRender: RenderProxy = RenderProxy()
) : FrameLayout(context, attrs), UCSContainerControl, UCSRenderControl by mRender {

    private val TAG: String = "[DisplayContainer@${this.hashCode()}]"

    //绑定的界面
    private var mBindActivityRef: SoftReference<Activity?>? = null

    //绑定的容器：即正常情况下显示播放器所属的容器
    private var mBindContainerRef: SoftReference<FrameLayout?>? = null

    //绑定的播放器
    private var mBindPlayerRef: SoftReference<UCSPlayerControl?>? = null

    /**
     * 控制器
     */
    var videoController: MediaController? = null
        private set

    val render: UCSRender get() = mRender

    private val mOnScreenModeChangeListeners =
        CopyOnWriteArrayList<UCSContainerControl.OnScreenModeChangeListener>()

    //屏幕模式切换帮助类
    private val mScreenModeHandler: ScreenModeHandler = ScreenModeHandler()

    //是否开启根据传感器获得的屏幕方向进入/退出全屏
    private var mEnableOrientationSensor = UCSPManager.isOrientationSensorEnabled

    //用户设置是否适配刘海屏
    private var mAdaptCutout = UCSPManager.isAdaptCutout

    //是否有刘海
    private var mHasCutout: Boolean? = null

    //刘海的高度
    private var mCutoutHeight = 0

    private val mPlayerEventListener = object : UCSPlayer.EventListener {
        override fun onInfo(what: Int, extra: Int) {
            try {
                plogi2(TAG) { "onInfo(what=$what,extra=$extra)" }
                when (what) {
                    UCSPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> {
                        plogi2(TAG) { "onInfo -> setVideoRotation($extra)" }
                        setVideoRotation(extra)
                    }
                }
            } catch (e: Throwable) {
                ploge2(TAG, e) {
                    "onInfo error()"
                }
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            plogi2(TAG) { "onVideoSizeChanged(width=$width,height=$height)" }
            setVideoSize(width, height)
        }
    }

    private val mDeviceOrientationChangedListener =
        object : DeviceOrientationSensorHelper.DeviceOrientationChangedListener {

            override fun onDeviceDirectionChanged(@DeviceOrientationSensorHelper.DeviceDirection direction: Int) {
                plogd2(TAG) { "onDeviceDirectionChanged(direction=$direction)" }
                when (direction) {
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_PORTRAIT -> {
                        //切换为竖屏
                        //todo lock lock的情况下不应该进行屏幕旋转
//                        //屏幕锁定的情况
//                        if (mLocked) return
                        //没有开启设备方向监听的情况
                        if (!mEnableOrientationSensor) return
                        plogd2(TAG) { "onDeviceDirectionChanged -> stopFullScreen" }
                        stopFullScreen()
                    }
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_LANDSCAPE -> {
                        plogd2(TAG) { "onDeviceDirectionChanged -> startFullScreen" }
                        startFullScreen()
                    }
                    DeviceOrientationSensorHelper.DEVICE_DIRECTION_LANDSCAPE_REVERSED -> {
                        plogd2(TAG) { "onDeviceDirectionChanged -> startFullScreen(true)" }
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

    private val mPlayerStateListener = UCSPlayer.OnPlayStateChangeListener { playState ->
        when (playState) {
            UCSPlayer.STATE_IDLE -> {
                mOrientationSensorHelper.disable()
            }
            UCSPlayer.STATE_PLAYING -> {
                if (!this@DisplayContainer.keepScreenOn)
                    this@DisplayContainer.keepScreenOn = true
            }
            UCSPlayer.STATE_PAUSED, UCSPlayer.STATE_PLAYBACK_COMPLETED, UCSPlayer.STATE_ERROR -> {
                if (this@DisplayContainer.keepScreenOn)
                    this@DisplayContainer.keepScreenOn = false
            }
        }
        plogd2(TAG) { "OnPlayStateChangeListener playState=${UCSPUtil.playState2str(playState)}($playState)" }
    }

    /**
     * 获取渲染视图的名字
     */
    val renderName: String get() = mRender.renderName

    /**
     * 当前屏幕模式：普通、全屏、小窗口
     */
    @ScreenMode
    override var screenMode: Int = ScreenMode.NORMAL
        internal set(@ScreenMode screenMode) {
            if (field != screenMode) {
                field = screenMode
                plogd2(TAG) { "screenMode changed,notify (screenMode=$screenMode)" }
                notifyScreenModeChanged(screenMode)
            }
        }

    /**
     * 绑定Activity
     */
    fun bindActivity(activity: Activity) {
        plogi2(TAG) { "bindActivity($activity)" }
        val bindActivity = getActivity()
        if (activity == bindActivity) {
            plogv2(TAG) { "bindActivity -> the activity is same with current activity. ignore set." }
            return
        }
        mBindActivityRef = SoftReference(activity)
        mOrientationSensorHelper.attachActivity(activity)
        if (bindActivity != null && isFullScreen()) {
            mScreenModeHandler.changeFullScreenActivity(bindActivity, activity, this)
        }
        plogi2(TAG) { "bindActivity($activity) -> current bind activity is ${getActivity()}" }
    }

    /**
     * 解绑Activity
     */
    fun unbindActivity() {
        plogi2(TAG) { "unbindActivity" }
        //todo 解绑activity 这个逻辑不好处理，假如当前view处于全屏或者小窗，接触窗口之后，下一步不一定是回到原来的container中，所以这里的逻辑只能是全屏或者小窗的时候，从parent中移除自己
        if(isFullScreen() || isTinyScreen()){
            removeFromParent()
        }
        mBindActivityRef = null
        mOrientationSensorHelper.detachActivity()
    }

    /**
     * 绑定所在的容器：即正常显示所在的容器
     */
    fun bindContainer(container: FrameLayout) {
        plogi2(TAG) { "bindContainer($container)" }
        if (container == mBindContainerRef?.get()) {
            plogv2(TAG) { "bindContainer -> the container is same with current bind container. ignore set." }
            return
        }
        mBindContainerRef = SoftReference(container)
        if (this.parent != container && isNormalScreen()) {
            //如果当前display所在的容器与指定容器不相同，并且处于普通模式，则从原来的容器中转移到当前容器
            removeFromParent()
            container.addView(this)
        }
    }

    /**
     * 解绑容器
     */
    fun unbindContainer() {
        plogi2(TAG) { "unbindContainer" }
        val bindContainer = mBindContainerRef?.get() ?: return
        if (bindContainer == this.parent) {
            removeFromParent()
        }
        mBindContainerRef = null
    }

    /**
     * 绑定播放器
     */
    override fun bindPlayer(player: UCSPlayerControl?) {
        plogi2(TAG) { "bindPlayer($player)" }
        val currentPlayer = mBindPlayerRef?.get()
        if (currentPlayer != player) {
            plogi2(TAG) { "bindPlayer($player) -> not same with current player,un ref current player($currentPlayer)." }
            currentPlayer?.let {
                it.removeEventListener(mPlayerEventListener)
                it.removeOnPlayStateChangeListener(mPlayerStateListener)
            }
            mBindPlayerRef = null
            if (player != null) {
                mBindPlayerRef = SoftReference(player)
                player.addEventListener(mPlayerEventListener)
                player.addOnPlayStateChangeListener(mPlayerStateListener)
            }
        } else {
            plogi2(TAG) { "bindPlayer($player) -> same with current player($currentPlayer),ignore ref player listeners.." }
        }
        mRender.bindPlayer(player)
        videoController?.setMediaPlayer(player)
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    override fun setVideoController(mediaController: MediaController?) {
        plogi2(TAG) { "setVideoController($mediaController)" }
        if (mediaController == videoController) {
            plogi2(TAG) { "setVideoController -> same with current video controller,set ignore." }
            return
        }
        removeController()
        videoController = mediaController
        mediaController?.let { controller ->
            if (controller.parent != this) {
                controller.removeFromParent()
                //添加控制器
                val params = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(controller, params)
            }
            controller.onAttachedToContainer(this)
            plogi2(TAG) { "setVideoController -> controller attached to display container." }
            //如果已经绑定了播放器，则将播放器传递给controller
            mBindPlayerRef?.get().let {
                controller.setMediaPlayer(it)
            }
        }
    }

    /**
     * 移除控制器
     * @return 被移除的控制器
     */
    private fun removeController(): MediaController? {
        val vc = videoController ?: return null
        if (vc.parent == this) {
            removeView(vc)
            vc.onDetachedFromContainer()
        }
        videoController = null
        return vc
    }

    /**
     * 通知当前界面模式发生了变化
     */
    private fun notifyScreenModeChanged(@ScreenMode screenMode: Int) {
        plogi2(TAG) { "notifyScreenModeChanged($screenMode)" }
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
            ScreenMode.NORMAL -> {
                if (mEnableOrientationSensor) {
                    mOrientationSensorHelper.enable()
                } else {
                    mOrientationSensorHelper.disable()
                }
                if (hasCutout()) {
                    adaptCutout(context, false)
                }
            }
            ScreenMode.FULL_SCREEN -> {
                //在全屏时强制监听设备方向
                mOrientationSensorHelper.enable()
                if (hasCutout()) {
                    adaptCutout(context, true)
                }
            }
            ScreenMode.TINY_SCREEN -> mOrientationSensorHelper.disable()
        }
    }

    /**
     * 设置是否适配刘海屏
     */
    override fun setAdaptCutout(adaptCutout: Boolean) {
        plogd2(TAG) { "setAdaptCutout($adaptCutout)" }
        mAdaptCutout = adaptCutout
    }

    /**
     * 是否有刘海屏
     */
    override fun hasCutout(): Boolean {
        return mHasCutout ?: false
    }

    /**
     * 刘海的高度
     */
    override fun getCutoutHeight(): Int {
        return mCutoutHeight
    }

    override fun setPlayerBackgroundColor(color: Int) {
        setBackgroundColor(color)
    }

    /**
     * 添加屏幕模式变化监听
     */
    override fun addOnScreenModeChangeListener(listener: UCSContainerControl.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.add(listener)
    }

    /**
     * 移除屏幕模式变化监听
     */
    override fun removeOnScreenModeChangeListener(listener: UCSContainerControl.OnScreenModeChangeListener) {
        mOnScreenModeChangeListeners.remove(listener)
    }

    override fun setEnableOrientationSensor(enable: Boolean) {
        plogd2(TAG) { "setEnableOrientationSensor($enable)" }
        mEnableOrientationSensor = enable
    }

    fun isNormalScreen(): Boolean {
        return screenMode == ScreenMode.NORMAL
    }

    override fun isFullScreen(): Boolean {
        return screenMode == ScreenMode.FULL_SCREEN
    }

    override fun isTinyScreen(): Boolean {
        return screenMode == ScreenMode.TINY_SCREEN
    }

    private inline fun getActivity(): Activity? = mBindActivityRef?.get()

    private inline fun requireActivity(methodName: () -> String): Activity {
        return mBindActivityRef?.get()
            ?: throw IllegalArgumentException("must invoke ${::bindActivity.name} method to bind activity before call method ${methodName.invoke()}")
    }

    private inline fun requireContainer(methodName: () -> String): FrameLayout {
        return mBindContainerRef?.get()
            ?: throw IllegalArgumentException("must invoke ${::bindContainer.name} method to bind container before call method ${methodName.invoke()}")
    }

    /**
     * 横竖屏切换
     * 如果切换为横屏、并且当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面；
     * 如果切换为竖屏，并且当前界面为界面间共享的播放器，则调用此方法前，需要先调用[bindContainer]绑定竖屏显示状态下的容器；并且也可以调用[bindActivity]指定当前的界面，用于退出全屏时设置Activity为竖屏
     *
     * @return
     */
    override fun toggleFullScreen(): Boolean {
        plogd2(TAG) { "toggleFullScreen()" }
        return if (isFullScreen()) {
            plogd2(TAG) { "toggleFullScreen() -> stopFullScreen" }
            stopFullScreen()
        } else {
            plogd2(TAG) { "toggleFullScreen() -> startFullScreen" }
            startFullScreen()
        }
    }

    /**
     * 开始全屏
     * 如果当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面；
     */
    override fun startFullScreen(isLandscapeReversed: Boolean): Boolean {
        plogd2(TAG) { "startFullScreen(isLandscapeReversed = $isLandscapeReversed)" }
        val activity = requireActivity {
            "startFullScreen"
        }
        if (isLandscapeReversed) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                plogd2(TAG) { "startFullScreen-> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE" }
                activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
        } else {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                plogd2(TAG) { "startFullScreen-> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE" }
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
        plogd2(TAG) { "startVideoViewFullScreen($tryHideSystemBar)" }
        if (isFullScreen()) {
            plogd2(TAG) { "startVideoViewFullScreen -> current is full screen ,return directly." }
            return false
        }
        val activity = requireActivity {
            "startVideoViewFullScreen"
        }
        if (mScreenModeHandler.startFullScreen(activity, this, tryHideSystemBar)) {
            plogd2(TAG) { "startVideoViewFullScreen -> startFullScreen success." }
            screenMode = ScreenMode.FULL_SCREEN
            return true
        }
        plogd2(TAG) { "startVideoViewFullScreen -> startFullScreen fail." }
        return false
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun stopFullScreen(): Boolean {
        plogd2(TAG) { "stopFullScreen()" }
        val activity = getActivity()
        if (activity != null && activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            plogd2(TAG) { "stopFullScreen -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT" }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return stopVideoViewFullScreen()
    }

    /**
     * 整个播放视图（Render、Controller）退出全屏（切换为竖屏）;则调用此方法前，需要先调用[bindContainer]绑定竖屏显示的容器；
     */
    override fun stopVideoViewFullScreen(tryShowSystemBar: Boolean): Boolean {
        plogd2(TAG) { "stopVideoViewFullScreen(tryShowSystemBar=$tryShowSystemBar)" }
        if (!isFullScreen()) {
            plogd2(TAG) { "stopVideoViewFullScreen -> current state is not full screen,return directly." }
            return false
        }
        val container = requireContainer {
            "stopVideoViewFullScreen"
        }
        val activity = getActivity()
        val changed = if (activity != null && tryShowSystemBar) {
            plogd2(TAG) { "stopVideoViewFullScreen -> mScreenModeHandler.stopFullScreen(activity, container, this)" }
            mScreenModeHandler.stopFullScreen(activity, container, this)
        } else {
            plogd2(TAG) { "stopVideoViewFullScreen -> mScreenModeHandler.stopFullScreen(container, this)" }
            mScreenModeHandler.stopFullScreen(container, this)
        }
        if (changed) {
            screenMode = ScreenMode.NORMAL
            return true
        }
        return false
    }

    /**
     * 开启小屏
     * 如果当前播放器为界面间共享的播放器，则调用此方法前，需要先调用[bindActivity]绑定界面
     */
    override fun startTinyScreen() {
        plogd2(TAG) { "startTinyScreen()" }
        if (isTinyScreen()) {
            plogd2(TAG) { "startTinyScreen -> current screen mode is tiny screen,return directly." }
            return
        }
        val activity = requireActivity {
            ::startTinyScreen.name
        }
        if (mScreenModeHandler.startTinyScreen(activity, this)) {
            plogd2(TAG) { "startTinyScreen -> success：mScreenModeHandler.startTinyScreen(activity, this)" }
            screenMode = ScreenMode.TINY_SCREEN
            return
        }

        plogd2(TAG) { "startTinyScreen -> fail." }
    }

    /**
     * 退出小屏;调用此方法前，需要先调用[bindContainer]绑定界面
     */
    override fun stopTinyScreen() {
        plogd2(TAG) { "stopTinyScreen()" }
        if (!isTinyScreen()) {
            plogd2(TAG) { "stopTinyScreen -> current  state is not in tiny screen ,return directly." }
            return
        }
        val container: ViewGroup = requireContainer {
            ::stopTinyScreen.name
        }
        if (mScreenModeHandler.stopTinyScreen(container, this)) {
            plogd2(TAG) { "startTinyScreen -> success：mScreenModeHandler.stopTinyScreen(container, this)" }
            screenMode = ScreenMode.NORMAL
            return
        }
        plogd2(TAG) { "stopTinyScreen -> fail" }
    }

    override fun addFocusables(views: ArrayList<View>, direction: Int) {
        plogd2(TAG) { "addFocusables" }
        val controller = videoController
        if (controller != null && controller.canTakeFocus) {
            plogd2(TAG) { "addFocusables -> controller can take focus ,only add it to array." }
            views.add(controller)//controller能够获取焦点的情况下，优先只让controller获取焦点
            return
        }
        plogd2(TAG) { "addFocusables -> super.addFocusables(views, direction)." }
        super.addFocusables(views, direction)
    }

    override fun onAttachedToWindow() {
        plogd2(TAG) { "onAttachedToWindow" }
        super.onAttachedToWindow()
        checkCutout()
    }

    /**
     * 检查是否需要适配刘海
     */
    private fun checkCutout() {
        if (!mAdaptCutout || mCutoutHeight > 0) {
            plogd2(TAG) { "checkCutout: adaptCutout = $mAdaptCutout cutoutHeight=$mCutoutHeight, return ." }
            return
        }

        val activity = getActivity()
        if (activity != null && mHasCutout == null) {
            mHasCutout = allowDisplayToCutout(activity)
            if (hasCutout()) {
                //竖屏下的状态栏高度可认为是刘海的高度
                mCutoutHeight = getStatusBarHeightPortrait(context).toInt()
            }
        }
        plogd2(TAG) { "checkCutout: adaptCutout = $mAdaptCutout cutoutHeight=$mCutoutHeight" }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        plogd2(TAG) { "onAttachedToWindow" }
        if (hasWindowFocus)
            autoInjectContainer()
        val player = mBindPlayerRef?.get() ?: return
        if (player.isPlaying() && (mEnableOrientationSensor || isFullScreen())) {
            if (hasWindowFocus) {
                postDelayed({ mOrientationSensorHelper.enable() }, 800)
            } else {
                mOrientationSensorHelper.disable()
            }
        }
    }

    private fun autoInjectContainer() {
        //布局中加载完成的时候，判断下是否需要自动绑定Container
        try {
            val parent = this.parent
            plogd2(TAG) { "autoInjectContainer -> bindContainer = ${mBindContainerRef?.get()} parent=$parent" }
            if (mBindContainerRef?.get() == null && parent is FrameLayout) {
                bindContainer(parent)
                plogd2(TAG) { "autoInjectContainer -> 自动注入了Container = ${mBindContainerRef?.get()} parent=$parent" }
            }
        } catch (e: Throwable) {
            ploge2(TAG, e) {
                "autoInjectContainer fail."
            }
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return videoController?.onBackPressed() ?: false
    }

    override fun release() {
        //关闭屏幕常亮
        keepScreenOn = false
        mRender.release()
        val player = mBindPlayerRef?.get()
        if (player != null) {
            mBindPlayerRef = null
            videoController?.setMediaPlayer(null)
        }
        mOrientationSensorHelper.disable()
        mOrientationSensorHelper.setDeviceOrientationChangedListener(null)
    }

    /**
     * 解析对应参数：一般是从外层容器从解析
     */
    private fun applyAttributes(context: Context, attrs: AttributeSet) {
        //读取xml中的配置，并综合全局配置
        val ta = context.obtainStyledAttributes(attrs, R.styleable.UCSDisplayContainer)
        if (ta.hasValue(R.styleable.UCSDisplayContainer_ucsp_screenScaleType)) {
            val screenAspectRatioType =
                ta.getInt(
                    R.styleable.UCSDisplayContainer_ucsp_screenScaleType,
                    UCSPManager.screenAspectRatioType
                )
            setAspectRatioType(screenAspectRatioType)
        }
        val playerBackgroundColor =
            ta.getColor(
                R.styleable.UCSDisplayContainer_ucsp_playerBackgroundColor,
                UCSPManager.defaultPlayerBackgroundColor
            )
        setPlayerBackgroundColor(playerBackgroundColor)
        ta.recycle()
    }


    init {
        if (attrs != null) {
            applyAttributes(context, attrs)
        } else {
            setPlayerBackgroundColor(UCSPManager.defaultPlayerBackgroundColor)
            //如果不是布局创建的，默认填充整个parent
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        //如果当前容器是通过Activity上下文构建的，则默认绑定的界面为该Activity
        val activity = UCSPUtil.getActivityContext(context)
        if (activity != null) {
            bindActivity(activity)
        }
        mRender.bindContainer(this)
    }
}