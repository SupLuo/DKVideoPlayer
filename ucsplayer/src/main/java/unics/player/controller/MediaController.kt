package unics.player.controller

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import unics.player.ScreenMode
import unics.player.UCSPManager
import unics.player.UCSVideoView
import unics.player.internal.*
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerControl

/**
 * 控制器基类：该类的职责是作为播放器与容器中各种小组件之间的纽带
 * 由于控制器一般是跟随播放器Render所在的容器，因此本类需要绑定所属的[UCSContainerControl],
 * 并绑定[UCSPlayerControl]用于各组件进行播放控制
 *
 * @see .show
 * @see .hide
 * @see .startFadeOut
 * @see .stopFadeOut
 * @see .setFadeOutTime
 * @see .setLocked
 * @see .setEnableOrientationSensor
 * @see .toggleFullScreen
 * @see .startFullScreen
 * @see .stopFullScreen
 * @see .setAdaptCutout
 * @see .hasCutout
 * @see .getCutoutHeight
 * @see .onVisibilityChanged
 * @see .onLockStateChanged
 * @see .onScreenModeChanged
 * @see .onProgressChanged
 */
open class MediaController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), UCSMediaController {

    private val TAG = "[MediaController@${this.hashCode()}]"

    //所关联的容器
    @JvmField
    protected var mBindContainerControl: UCSContainerControl? = null

    //绑定的播放器
    @JvmField
    protected var mBindPlayer: UCSPlayerControl? = null

    //当前控制器中保存的所有控制组件
    @JvmField
    protected val mControlComponents = LinkedHashMap<ControlComponent, Boolean>()

    override var isLocked: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            notifyLockStateChanged(value)
        }

    /**
     * 当前播放器状态
     */
    @UCSPlayer.PlayState
    private var mPlayState = UCSPlayer.STATE_IDLE
        set(value) {
            if (field == value)
                return
            field = value
            when (value) {
                UCSPlayer.STATE_IDLE -> {
                    isLocked = false
                    this.isShowing = false
                    //由于游离组件是独立于控制器存在的，
                    //所以在播放器release的时候需要移除
                    removeAllDissociateComponent()
                }
                UCSPlayer.STATE_PLAYBACK_COMPLETED -> {
                    isLocked = false
                    this.isShowing = false
                }
                UCSPlayer.STATE_ERROR -> this.isShowing = false
            }
            for ((key) in mControlComponents) {
                key.onPlayStateChanged(value)
            }
            onPlayerStateChanged(value)
        }


    //显示动画
    private val mShowAnim: Animation = AlphaAnimation(0f, 1f).also {
        it.duration = 300
    }

    //隐藏动画
    private val mHideAnim: Animation = AlphaAnimation(1f, 0f).also {
        it.duration = 300
    }

    /**
     * 控制器显示超时时间：即显示超过该时间后自动隐藏
     */
    private var mDefaultTimeout = 4000L

    /**
     * 自动隐藏的Runnable
     */
    private val mFadeOut = Runnable { hide() }

    /**
     * 是否开始刷新进度
     */
    private var mProgressRefreshing = false

    /**
     * 刷新进度Runnable
     */
    private val progressUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            val pos = updateProgress()
            if (isPlaying()) {

                var divisor = mBindPlayer?.getSpeed() ?: 1f
                if (divisor == 0f) {
                    divisor = 1f
                }
                postDelayed(
                    this,
                    ((1000 - pos % 1000) / divisor).toLong()
                )
            } else {
                mProgressRefreshing = false
            }
        }
    }

    /**
     * 控制器是否处于显示状态
     */
    override var isShowing: Boolean = false
        protected set

    @JvmField
    protected var mActivity: Activity? = null

    @ScreenMode
    private var mScreenMode: Int = ScreenMode.UNKNOWN
        set(value) {
            if (field == value)
                return
            field = value
            //非未知才通知变化
            if (value != ScreenMode.UNKNOWN)
                onScreenModeChanged(value)
        }

    val playerControl: UCSPlayerControl? get() = mBindPlayer

    val containerControl: UCSContainerControl? get() = mBindContainerControl

    protected inline fun isPlaying(): Boolean {
        return mBindPlayer?.isPlaying() == true
    }

    /**
     * 是否处于播放状态
     *
     * @return
     */
    protected val isInPlaybackState: Boolean
        get() = mBindPlayer != null
                && mPlayState != UCSPlayer.STATE_ERROR
                && mPlayState != UCSPlayer.STATE_IDLE
                && mPlayState != UCSPlayer.STATE_PREPARING
                && mPlayState != UCSPlayer.STATE_PREPARED
                && mPlayState != UCSPlayer.STATE_PREPARED_BUT_ABORT
                && mPlayState != UCSPlayer.STATE_PLAYBACK_COMPLETED

    protected val isInCompleteState: Boolean get() = mPlayState == UCSPlayer.STATE_PLAYBACK_COMPLETED

    protected val isInErrorState: Boolean get() = mPlayState == UCSPlayer.STATE_ERROR

    private val mScreenModeChangeListener =
        UCSContainerControl.OnScreenModeChangeListener { screenMode ->
            mScreenMode = screenMode
        }

    private val mPlayStateChangeListener = UCSPlayer.OnPlayStateChangeListener { playState ->
        plogd2(TAG) { "OnPlayStateChangeListener -> playState = ${UCSPUtil.playState2str(playState)}($playState)" }
        mPlayState = playState
    }

    /**
     * 绑定所属的容器
     */
    @CallSuper
    open fun bindContainer(container: UCSContainerControl) {
        plogd2(TAG) { "bindContainer($container)" }
        if (mBindContainerControl == container) {
            plogd2(TAG) { "bindContainer -> container is same with current, ignore set." }
            return
        }
        plogd2(TAG) { "bindContainer -> container is changed, add screen mode listener and update screen mode immediately" }
        mBindContainerControl = container
        container.removeOnScreenModeChangeListener(mScreenModeChangeListener)
        container.addOnScreenModeChangeListener(mScreenModeChangeListener)
        //更新一次
        mScreenMode = container.screenMode
    }

    @CallSuper
    open fun unbindContainer() {
        plogd2(TAG) { "unbindContainer -> remove screen mode listener ,and set current screen mode to unknown." }
        mBindContainerControl?.removeOnScreenModeChangeListener(mScreenModeChangeListener)
        mBindContainerControl = null
        mScreenMode = ScreenMode.UNKNOWN
    }

    /**
     * 重要：此方法用于将[UCSVideoView] 和控制器绑定
     */
    @CallSuper
    open fun setMediaPlayer(mediaPlayer: UCSPlayerControl?) {
        plogd2(TAG) { "setMediaPlayer($mediaPlayer)" }
        if (mBindPlayer == mediaPlayer) {
            plogd2(TAG) { "setMediaPlayer -> media player is same with current, ignore set." }
            return
        }

        val prev = mBindPlayer
        if (prev != null) {
            plogd2(TAG) { "setMediaPlayer -> prev media player is not null ,unbind it:remove play state change listener and notify to control components." }
            prev.removeOnPlayStateChangeListener(mPlayStateChangeListener)
            for ((component) in mControlComponents) {
                component.onPlayerDetached(prev)
            }
        }
        mBindPlayer = mediaPlayer
        //绑定ControlComponent和Controller
        if (mediaPlayer != null) {
            plogd2(TAG) { "setMediaPlayer -> bind media player : add play state change listener and notify to control components." }
            mediaPlayer.addOnPlayStateChangeListener(mPlayStateChangeListener)
            for ((component) in mControlComponents) {
                component.onPlayerAttached(mediaPlayer)
            }
            mPlayState = mediaPlayer.currentState
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //自动注入上级容器
        if (mBindContainerControl == null) {
            val parent = this.parent
            if (parent is UCSContainerControl) {
                plogd2(TAG) { "onAttachedToWindow -> try auto inject container control." }
                bindContainer(parent)
            }
        }
    }

    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     */
    fun addControlComponent(vararg component: ControlComponent) {
        plogd2(TAG) { "" }
        component.forEach {
            addControlComponent(it, false)
        }
    }

    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     * @param isDissociate 是否为游离的控制组件，
     * 如果为 true ControlComponent 不会添加到控制器中，ControlComponent 将独立于控制器而存在，
     * 如果为 false ControlComponent 将会被添加到控制器中，并显示出来。
     * 为什么要让 ControlComponent 将独立于控制器而存在，假设有如下几种情况：
     * 情况一：
     * 如果在一个列表中控制器是复用的，但是控制器的某些部分是不能复用的，比如封面图，
     * 此时你就可以将封面图拆分成一个游离的 ControlComponent，并把这个 ControlComponent
     * 放在 item 的布局中，就可以实现每个item的封面图都是不一样，并且封面图可以随着播放器的状态显示和隐藏。
     * demo中演示的就是这种情况。
     * 情况二：
     * 假设有这样一种需求，播放器控制区域在显示区域的下面，此时你就可以通过自定义 ControlComponent
     * 并将 isDissociate 设置为 true 来实现这种效果。
     */
    @MainThread
    fun addControlComponent(component: ControlComponent, isDissociate: Boolean) {
        mControlComponents[component] = isDissociate
        if (!isDissociate) {
            component.getView()?.let {
                if (UCSPManager.isControlIndexRevers) {
                    addView(it, 0)
                } else {
                    addView(it)
                }
            }
        }
        component.onControllerAttached(this)
    }

    /**
     * 移除某个控制组件
     */
    @MainThread
    fun removeControlComponent(component: ControlComponent) {
        removeControlComponentView(component)
        mControlComponents.remove(component)
    }

    /**
     * 移除某个控制组件
     */
    @MainThread
    fun removeControlComponent(vararg component: ControlComponent) {
        component.forEach {
            removeControlComponent(it)
        }
    }

    /**
     * 移除所有控制组件
     */
    @MainThread
    fun removeAllControlComponent() {
        for ((key) in mControlComponents) {
            removeControlComponentView(key)
        }
        mControlComponents.clear()
    }

    /**
     * 移除所有的游离控制组件
     * 关于游离控制组件的定义请看 [.addControlComponent] 关于 isDissociate 的解释
     */
    fun removeAllDissociateComponent() {
        val it: MutableIterator<Map.Entry<*, Boolean>> = mControlComponents.iterator()
        while (it.hasNext()) {
            val (_, value) = it.next()
            if (value) {
                it.remove()
            }
        }
    }

    /**
     * 从当前控制器中移除添加的控制器view
     *
     * @param component
     */
    private fun removeControlComponentView(component: ControlComponent) {
        val view = component.getView() ?: return
        removeView(view)
    }

    /***********START 关键方法代码 */
    override val isFullScreen: Boolean
        get() = mBindContainerControl?.isFullScreen() ?: false

    /**
     * 横竖屏切换
     */
    override fun toggleFullScreen(): Boolean {
        return mBindContainerControl?.toggleFullScreen() ?: false
    }

    override fun startFullScreen(isLandscapeReversed: Boolean): Boolean {
        return mBindContainerControl?.startFullScreen(isLandscapeReversed) ?: false
    }

    override fun stopFullScreen(): Boolean {
        return mBindContainerControl?.stopFullScreen() ?: false
    }

    /**
     * 显示播放视图
     */
    override fun show() {
        startFadeOut()
        if (this.isShowing) return
        handleVisibilityChanged(true, mShowAnim)
        this.isShowing = true
    }

    /**
     * 隐藏播放视图
     */
    override fun hide() {
        stopFadeOut()
        if (!this.isShowing) return
        handleVisibilityChanged(false, mHideAnim)
        this.isShowing = false
    }

    /**
     * 设置自动隐藏倒计时持续的时间
     *
     * @param timeout 默认4000，比如大于0才会生效
     */
    override fun setFadeOutTime(@IntRange(from = 1) timeout: Int) {
        if (timeout > 0) {
            mDefaultTimeout = timeout.toLong()
        }
    }

    /**
     * 开始倒计时隐藏控制器
     */
    override fun startFadeOut() {
        //重新开始计时
        stopFadeOut()
        postDelayed(mFadeOut, mDefaultTimeout)
    }

    /**
     * 移除控制器隐藏倒计时
     */
    override fun stopFadeOut() {
        removeCallbacks(mFadeOut)
    }

    /**
     * 开始刷新进度，注意：需在STATE_PLAYING时调用才会开始刷新进度
     */
    override fun startUpdateProgress() {
        if (mProgressRefreshing) return
        post(progressUpdateRunnable)
        mProgressRefreshing = true
    }

    /**
     * 停止刷新进度
     */
    override fun stopUpdateProgress() {
        if (!mProgressRefreshing) return
        removeCallbacks(progressUpdateRunnable)
        mProgressRefreshing = false
    }

    /**
     * 显示移动网络播放提示
     *
     * @return 返回显示移动网络播放提示的条件，false:不显示, true显示
     * 此处默认根据手机网络类型来决定是否显示，开发者可以重写相关逻辑
     */
    open fun showNetWarning(): Boolean {
        return (context.getNetworkType() == NETWORK_MOBILE && !UCSPManager.isPlayOnMobileNetwork)
    }

    /**
     * 播放和暂停
     */
    fun togglePlay() {
        invokeOnPlayerAttached {
            if (it.isPlaying()) {
                it.pause()
            } else {
                it.start()
            }
        }
    }

    /**
     * @return true:调用了重播方法，false则表示未处理任何
     */
    fun replay(resetPosition: Boolean = true) {
        mBindPlayer?.replay(resetPosition)
    }

    //------------------------ start handle event change ------------------------//

    private fun handleVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!isLocked) { //没锁住时才向ControlComponent下发此事件
            for ((component) in mControlComponents) {
                component.onVisibilityChanged(isVisible, anim)
            }
        }
        onVisibilityChanged(isVisible, anim)
    }

    /**
     * 更新当前播放进度
     * @return 当前播放位置
     */
    private fun updateProgress(): Int {
        val player = mBindPlayer ?: return 0
        val position = player.getCurrentPosition().toInt()
        val duration = player.getDuration().toInt()
        for ((component) in mControlComponents) {
            component.onProgressChanged(duration, position)
        }
        onProgressChanged(duration, position)
        return position
    }

    /**
     * 通知锁定状态发生了变化
     */
    private fun notifyLockStateChanged(isLocked: Boolean) {
        for ((component) in mControlComponents) {
            component.onLockStateChanged(isLocked)
        }
        onLockStateChanged(isLocked)
    }


    /**
     * 子类重写此方法监听控制的显示和隐藏
     *
     * @param isVisible 是否可见
     * @param anim      显示/隐藏动画
     */
    protected open fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}

    /**
     * 用于子类重写
     * 刷新进度回调，子类可在此方法监听进度刷新，然后更新ui
     *
     * @param duration 视频总时长
     * @param position 视频当前播放位置
     */
    protected open fun onProgressChanged(duration: Int, position: Int) {}

    /**
     * 用于子类重写
     */
    @CallSuper
    protected open fun onScreenModeChanged(screenMode: Int) {
        //通知界面模式发生改变
        for ((component) in mControlComponents) {
            component.onScreenModeChanged(screenMode)
        }
    }

    /**
     * 用于子类重写
     */
    protected open fun onPlayerStateChanged(@UCSPlayer.PlayState playState: Int) {}

    /**
     * 用于子类重写
     */
    protected open fun onLockStateChanged(isLocked: Boolean) {}

    /**
     * 改变返回键逻辑，用于activity
     */
    open fun onBackPressed(): Boolean {
        return false
    }

    //------------------------ end handle event change ------------------------//

    /**
     * 切换显示/隐藏状态
     */
    fun toggleShowState() {
        if (this.isShowing) {
            hide()
        } else {
            show()
        }
    }

    fun toggleLock() {
        isLocked = !isLocked
    }

    protected inline fun <R> invokeOnPlayerAttached(
        showToast: Boolean = true,
        block: (UCSPlayerControl) -> R
    ): R? {
        val player = mBindPlayer
        if (player == null) {
            if (showToast) {
                toast("请先调用setMediaPlayer方法绑定播放器.")
            }
            plogw2("MediaController") {
                "error on ${Thread.currentThread().stackTrace[2].methodName} method invoke.but throwable is ignored."
            }
            return null
        }
        return block.invoke(player)
    }

    protected inline fun <R> invokeOnContainerAttached(
        showToast: Boolean = true,
        block: (UCSContainerControl) -> R
    ): R? {
        val control = mBindContainerControl
        if (control == null) {
            if (showToast) {
                toast("请先调用${::bindContainer.name}方法绑定所在的容器.")
            }
            plogw2("MediaController") {
                "error on ${Thread.currentThread().stackTrace[2].methodName} method invoke.but throwable is ignored."
            }
            return null
        }
        return block.invoke(control)
    }

//    /**
//     * 横竖屏切换，根据适配宽高决定是否旋转屏幕
//     */
//    open fun toggleFullScreenByVideoSize(activity: Activity?) {
//        if (activity == null || activity.isFinishing) return
//        val size: IntArray = getVideoSize()
//        val width = size[0]
//        val height = size[1]
//        if (isFullScreen) {
//            stopVideoViewFullScreen()
//            if (width > height) {
//                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//            }
//        } else {
//            startVideoViewFullScreen()
//            if (width > height) {
//                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//            }
//        }
//    }

    init {
        mActivity = UCSPUtil.getActivityContext(context)
    }

    fun release() {
        removeAllControlComponent()
    }
}