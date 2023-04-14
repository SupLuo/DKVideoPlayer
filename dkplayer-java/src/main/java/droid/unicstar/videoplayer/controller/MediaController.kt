package droid.unicstar.videoplayer.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.IntRange
import droid.unicstar.videoplayer.*
import droid.unicstar.videoplayer.orDefault
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.removeAllByValue
import droid.unicstar.videoplayer.toast
import xyz.doikki.videoplayer.*
import xyz.doikki.videoplayer.controller.VideoViewController
import xyz.doikki.videoplayer.controller.component.ControlComponent
import xyz.doikki.videoplayer.util.*

/**
 * 控制器基类
 * 对外提供可以控制的方法
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
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    VideoViewController{

    /**
     * 当前控制器中保存的所有控制组件
     */
    @JvmField
    protected val mControlComponents = LinkedHashMap<ControlComponent, Boolean>()

    /**
     * 绑定的播放器
     */
    protected var mPlayer: UNSVideoViewControl? = null

    //是否处于锁定状态
    private var mLocked = false

    /**
     * 当前播放器状态
     */
    @UNSPlayer.PlayState
    private var mPlayerState = 0

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
            if (mPlayer?.isPlaying().orDefault()) {
                postDelayed(this, ((1000 - pos % 1000) / mPlayer?.getSpeed().orDefault(1f)).toLong())
            } else {
                mProgressRefreshing = false
            }
        }
    }

    /**
     * 控制器是否处于显示状态
     */
    protected var mShowing = false

    @JvmField
    protected var mActivity: Activity? = null

    val playerControl: UNSVideoViewControl? get() = mPlayer

    init {

        mActivity = PlayerUtils.scanForActivity(context)
    }

    /**
     * 是否处于播放状态
     *
     * @return
     */
    protected val isInPlaybackState: Boolean
        get() = mPlayer != null
                && mPlayerState != UNSPlayer.STATE_ERROR
                && mPlayerState != UNSPlayer.STATE_IDLE
                && mPlayerState != UNSPlayer.STATE_PREPARING
                && mPlayerState != UNSPlayer.STATE_PREPARED
                && mPlayerState != UNSPlayer.STATE_PREPARED_BUT_ABORT
                && mPlayerState != UNSPlayer.STATE_PLAYBACK_COMPLETED

    protected val isInCompleteState: Boolean get() = mPlayerState == UNSPlayer.STATE_PLAYBACK_COMPLETED

    protected val isInErrorState: Boolean get() = mPlayerState == UNSPlayer.STATE_ERROR

    /**
     * 重要：此方法用于将[UNSVideoView] 和控制器绑定
     */
    @CallSuper
    open fun setMediaPlayer(mediaPlayer: UNSVideoViewControl) {
        mPlayer = mediaPlayer
        //绑定ControlComponent和Controller
        for ((component) in mControlComponents) {
            component.onPlayerAttached(mediaPlayer)
        }
    }
    /***********START 关键方法代码 */
    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     */
    fun addControlComponent(vararg component: ControlComponent) {
        for (item in component) {
            addControlComponent(item, false)
        }
    }

    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     *
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
    fun addControlComponent(component: ControlComponent, isDissociate: Boolean) {
        mControlComponents[component] = isDissociate
        component.attachController(this)
        val view = component.getView()
        if (view != null && !isDissociate) {
            addView(view, 0)
        }
    }

    /**
     * 移除某个控制组件
     */
    fun removeControlComponent(component: ControlComponent) {
        removeControlComponentView(component)
        mControlComponents.remove(component)
    }

    /**
     * 移除所有控制组件
     */
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
    fun removeAllDissociateComponents() {
        mControlComponents.removeAllByValue {
            it
        }
//
//        val it: MutableIterator<Map.Entry<ControlComponent, Boolean>> =
//            mControlComponents.entries.iterator()
//        while (it.hasNext()) {
//            val (_, value) = it.next()
//            if (value) {
//                it.remove()
//            }
//        }
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
    /***********END 关键方法代码 */

    /***********START 关键方法代码 */
    override fun isFullScreen(): Boolean {
        return invokeOnPlayerAttached {
            it.isFullScreen()
        }.orDefault()
    }

    /**
     * 横竖屏切换
     */
    override fun toggleFullScreen(): Boolean {
        return invokeOnPlayerAttached {
            it.toggleFullScreen()
        }.orDefault()
    }

    override fun startFullScreen(isLandscapeReversed: Boolean): Boolean {
        return invokeOnPlayerAttached {
            it.startFullScreen(isLandscapeReversed)
        }.orDefault()
    }

    override fun stopFullScreen(): Boolean {
        return invokeOnPlayerAttached {
            it.stopFullScreen()
        }.orDefault()
    }

    /**
     * 设置锁定状态
     *
     * @param locked 是否锁定
     */
    override fun setLocked(locked: Boolean) {
        mLocked = locked
        notifyLockStateChanged(locked)
    }

    /**
     * 判断是否锁定
     *
     * @return true:当前已锁定界面
     */
    override fun isLocked(): Boolean {
        return mLocked
    }

    /**
     * 设置当前[UNSVideoView]界面模式：竖屏、全屏、小窗模式等
     * 是当[UNSVideoView]修改视图之后，调用此方法向控制器同步状态
     */
    @CallSuper
    open fun setScreenMode(@ScreenMode screenMode: Int) {
        notifyScreenModeChanged(screenMode)
    }

    /**
     * call by [UNSVideoView],设置播放器当前播放状态
     */
    @SuppressLint("SwitchIntDef")
    @CallSuper
    fun setPlayerState(@UNSPlayer.PlayState playState: Int) {
        mPlayerState = playState
        for ((key) in mControlComponents) {
            key.onPlayStateChanged(playState)
        }
        when (playState) {
            UNSPlayer.STATE_IDLE -> {
                mLocked = false
                mShowing = false
                //由于游离组件是独立于控制器存在的，
                //所以在播放器release的时候需要移除
                removeAllDissociateComponents()
            }
            UNSPlayer.STATE_PLAYBACK_COMPLETED -> {
                mLocked = false
                mShowing = false
            }
            UNSPlayer.STATE_ERROR -> mShowing = false
        }
        onPlayerStateChanged(playState)
    }

    /**
     * 控制器是否已隐藏
     *
     * @return
     */
    override fun isShowing(): Boolean {
        return mShowing
    }

    /**
     * 显示播放视图
     */
    override fun show() {
        startFadeOut()
        if (mShowing) return
        handleVisibilityChanged(true, mShowAnim)
        mShowing = true
    }

    /**
     * 隐藏播放视图
     */
    override fun hide() {
        stopFadeOut()
        if (!mShowing) return
        handleVisibilityChanged(false, mHideAnim)
        mShowing = false
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
        return (PlayerUtils.getNetworkType(context) == PlayerUtils.NETWORK_MOBILE && !DKManager.isPlayOnMobileNetwork)
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
    fun replay(resetPosition: Boolean = true): Boolean {
        return invokeOnPlayerAttached {
            it.replay(resetPosition)
            true
        }.orDefault(false)
    }


    //------------------------ start handle event change ------------------------//

    private fun handleVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!mLocked) { //没锁住时才向ControlComponent下发此事件
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
        val player = mPlayer ?: return 0
        val position = player.getCurrentPosition().toInt()
        val duration = player.getDuration().toInt()
        for ((component) in mControlComponents) {
            component.onProgressChanged(duration, position)
        }
        onProgressChanged(duration, position)
        return position
    }

    /**
     * 通知界面模式发生改变
     *
     * @param screenMode
     */
    private fun notifyScreenModeChanged(@ScreenMode screenMode: Int) {
        for ((component) in mControlComponents) {
            component.onScreenModeChanged(screenMode)
        }

        onScreenModeChanged(screenMode)
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
    protected open fun onScreenModeChanged(screenMode: Int) {}

    /**
     * 用于子类重写
     */
    protected open fun onPlayerStateChanged(@UNSPlayer.PlayState playState: Int) {}

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
        if (isShowing) {
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
        block: (UNSVideoViewControl) -> R
    ): R? {
        val player = mPlayer
        if (player == null) {
            if (showToast)
                toast("请先调用setMediaPlayer方法绑定播放器.")
            return null
        }
        return block.invoke(player)
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
}