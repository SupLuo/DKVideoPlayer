package unics.player.control.leanback

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.LayoutRes
import droid.unicstar.player.ui.TVCompatible
import unics.player.control.VodControlComponent
import unics.player.controller.KeyControlComponent
import unics.player.internal.plogi2
import xyz.doikki.videocontroller.R

/**
 * 点播底部控制栏
 */
@TVCompatible(message = "TV上不显示全屏按钮")
open class LeanbackVodControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = R.layout.ucsp_ctrl_vod_control_component_leanback
) : VodControlComponent(context, attrs, defStyleAttr, layoutId), KeyControlComponent {
    //
//    companion object {
//
//        private const val TAG = "LeanbackVodControlComponent"
//
//        /**
//         * 开始pending seek
//         */
//        private const val WHAT_BEGIN_PENDING_SEEK = 0x10
//
//        /**
//         * 取消pending seek
//         */
//        private const val WHAT_CANCEL_PENDING_SEEK = 0x11
//
//        /**
//         * 执行pending seek
//         */
//        private const val WHAT_HANDLE_PENDING_SEEK = 0x12
//
//        /**
//         * 更新pending seek的位置
//         */
//        private const val WHAT_UPDATE_PENDING_SEEK_POSITION = 0x13
//    }
//
//    /**
//     * 是否已经触发过pending seek的意图
//     */
//    private var mHasDispatchPendingSeek: Boolean = false
//
//    /**
//     * 当前待seek的位置
//     */
//    private var mCurrentPendingSeekPosition: Int = 0
//
//    private val seekCalculator: PendingSeekCalculator = RatioStepSeekCalculator()
//
//    /**
//     * 是否处理KeyEvent
//     */
//    var keyEventEnable: Boolean = true
//
//    /**
//     * 是否是直播
//     */
//    var isLive: Boolean = false
//
//    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
//
//        override fun handleMessage(msg: Message) {
//            super.handleMessage(msg)
//            when (msg.what) {
//                WHAT_BEGIN_PENDING_SEEK -> {
//                    invokeOnPlayerAttached { player ->
//                        val duration = player.getDuration().toInt()
//                        val currentPosition = player.getCurrentPosition().toInt()
//                        val event = msg.obj as KeyEvent
//                        mCurrentPendingSeekPosition = currentPosition
//                        onStartLeftOrRightKeyPressedForSeeking(event)
//                        seekCalculator.prepareCalculate(event, currentPosition, duration, width)
//                    }
//                }
//                WHAT_CANCEL_PENDING_SEEK -> {
//                    cancelPendingSeek()
//                    onCancelLeftOrRightKeyPressedForSeeking(msg.obj as KeyEvent)
//                }
//                WHAT_UPDATE_PENDING_SEEK_POSITION -> {
//                    invokeOnPlayerAttached { player ->
//                        val duration = player.getDuration().toInt()
//                        val event = msg.obj as KeyEvent
//                        val previousPosition = mCurrentPendingSeekPosition
//                        val incrementTimeMs =
//                            seekCalculator.calculateIncrement(
//                                event,
//                                previousPosition,
//                                duration,
//                                width
//                            )
//                        mCurrentPendingSeekPosition =
//                            (mCurrentPendingSeekPosition + incrementTimeMs)
//                                .coerceAtLeast(0)
//                                .coerceAtMost(duration)
//
//                        setPendingSeekPositionAndNotify(
//                            mCurrentPendingSeekPosition,
//                            previousPosition,
//                            duration
//                        )
//                        plogv2(TAG) {
//                            "update pending seek : action=${event.action}  eventTime=${event.eventTime - event.downTime} increment=${incrementTimeMs} previousPosition=${previousPosition} newPosition=${mCurrentPendingSeekPosition}"
//                        }
//                        /**
//                         * 发送一个延迟消息，用于某些红外遥控器或者设备按键事件分发顺序不一致的问题：
//                         * 即本身期望在[KeyEvent.ACTION_UP]的时候执行最终的seek动作，但是可能存在down事件还没有处理完的时候，系统已经接收了up事件，并且up事件没有下发到dispatchKeyEvent中
//                         */
//                        sendPendingSeekHandleMessage(event, 1500)
//                    }
//
//                }
//                WHAT_HANDLE_PENDING_SEEK -> {
//                    val event = msg.obj as KeyEvent
//                    //先做stop，再seek，避免loading指示器和seek指示器同时显示
//                    onStopLeftOrRightKeyPressedForSeeking(event)
//                    handlePendingSeek()
//                }
//            }
//        }
//    }
//
//    init {
//        //设置可以获取焦点
//        isFocusable = true
//        isFocusableInTouchMode = true
//        descendantFocusability = FOCUS_BEFORE_DESCENDANTS
//    }
//
//    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//        val controller = mController
//        val player = controller?.playerControl
//        if (!keyEventEnable || player == null)
//            return super.dispatchKeyEvent(event)
//
//        plogv2(TAG) {
//            "dispatchKeyEvent -> keyCode = ${event.keyCode}  action = ${event.action} repeatCount = ${event.repeatCount} isInPlaybackState=${player.currentState.isInPlaybackState} " +
//                    "isShowing=${controller.isShowing} mHasDispatchPendingSeek=${mHasDispatchPendingSeek} mCurrentPendingSeekPosition=${mCurrentPendingSeekPosition}"
//        }
//        val keyCode = event.keyCode
//        val uniqueDown = (event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN)
//        when (keyCode) {
//            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {//返回键&菜单键逻辑
//                if (uniqueDown && controller.isShowing) {
//                    //如果当前显示了控制器，则隐藏；
//                    controller.hide()
//                    return true
//                }
//                return false
//            }
//            KeyEvent.KEYCODE_HEADSETHOOK,
//            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
//            KeyEvent.KEYCODE_SPACE,
//            KeyEvent.KEYCODE_ENTER,
//            KeyEvent.KEYCODE_DPAD_CENTER
//            -> {//播放/暂停切换键
//                if (isLive) {
//                    if (uniqueDown) {//没有在播放中，则开始播放
//                        invokeOnPlayerAttached(showToast = false) {
//                            if (it.isInCompleteState) {
//                                controller.replay(resetPosition = true)
//                            } else if (it.isInErrorState) {
//                                controller.replay(resetPosition = false)
//                            }
//                        }
//                        controller.show()
//                    }
//                } else {
//                    if (uniqueDown) {  //第一次按下Ok键/播放暂停键/空格键
//                        if (player.isInPlaybackState) {
//                            //正在播放过程中，则切换播放
//                            controller.togglePlay()
//                        } else if (player.isInCompleteState) {
//                            controller.replay(resetPosition = true)
//                        } else if (player.isInErrorState) {
//                            controller.replay(resetPosition = false)
//                        }
//                        controller.show()
//                    }
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_MEDIA_PLAY -> {//播放键
//                if (uniqueDown && !player.isInPlaybackState) {//没有在播放中，则开始播放
//                    invokeOnPlayerAttached(showToast = false) { player ->
//                        player.start()
//                    }
//                    controller.show()
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_MEDIA_STOP,
//            KeyEvent.KEYCODE_MEDIA_PAUSE -> {//暂停键
//                if (!isLive && uniqueDown && player.isInPlaybackState) {
//                    invokeOnPlayerAttached(showToast = false) { player ->
//                        player.pause()
//                    }
//                    controller.show()
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_VOLUME_DOWN,
//            KeyEvent.KEYCODE_VOLUME_UP,
//            KeyEvent.KEYCODE_VOLUME_MUTE,
//            KeyEvent.KEYCODE_CAMERA
//            -> {//系统功能键
//                // don't show the controls for volume adjustment
//                //系统会显示对应的UI
//                return super.dispatchKeyEvent(event)
//            }
//            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> {//左右键，做seek行为
//                if (isLive) {
//                    plogv2(TAG) { "is live mode ,not response dpad left and right key event." }
//                    return false
//                }
//
//                plogv2(TAG) { "handle dpad right or left key for pending seek." }
//                if (!(seekEnabled && player.isInPlaybackState)) {//不允许拖动
//                    plogv2(TAG) { "pending seek disabled." }
//                    if (mHasDispatchPendingSeek) {
//                        mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)
//                        mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
//                        mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
//                        mHandler.sendMessage(
//                            mHandler.obtainMessage(
//                                WHAT_CANCEL_PENDING_SEEK,
//                                event
//                            )
//                        )
//                        plogv2(TAG) { "has dispatched pending seek,cancel it." }
//                        mHasDispatchPendingSeek = false
//                    }
//                    return true
//                }
//                if (uniqueDown && !controller.isShowing) {
//                    //第一次按下down并且当前控制器没有显示的情况下，只显示控制器
//                    plogv2(TAG) { "first pressed left/right dpad and controller is not showing,show it only." }
//                    controller.show()
//                    return true
//                }
//                //后续的逻辑存在以下几种情况：
//                //1、第一次按下down，并且控制已经显示，此时应该做seek动作
//                //2、执行up（存在可能已经有seek动作，或者没有seek动作：即按下down之后，立马执行了up）
//                //3、第N次按下down（n >1 ）
//
//                if (event.action == KeyEvent.ACTION_UP && !mHasDispatchPendingSeek) {
//                    //按下down之后执行了up，相当于只按了一次方向键，
//                    // 并且没有执行过pending行为（即单次按键的时候控制器还未显示，控制器已经显示的情况下单次按键是有效的行为），不做seek动作
//                    plogv2(TAG) { "left/right dpad key up, but not dispatch pending seek,return directly." }
//                    return true
//                }
//                dispatchPendingSeek(event)
//                return true
//            }
//            else -> {
//                controller.show()
//                return super.dispatchKeyEvent(event)
//            }
//        }
//    }
//
//    /**
//     * 处理按键拖动
//     */
//    private fun dispatchPendingSeek(event: KeyEvent) {
//        plogv2(TAG) { "dispatchPendingSeek" }
//        invokeOnPlayerAttached(showToast = false) { player ->
//            if (event.action == KeyEvent.ACTION_DOWN) {
//                if (!mHasDispatchPendingSeek) {
//                    mHasDispatchPendingSeek = true
//                    mHandler.removeMessages(WHAT_BEGIN_PENDING_SEEK)
//                    mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
//                    mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
//                    mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)
//
//                    mHandler.sendMessage(mHandler.obtainMessage(WHAT_BEGIN_PENDING_SEEK, event))
//                    plogv2(TAG) { "dispatchPendingSeek -> dispatch pending seek start." }
//                }
//                //更新pending seek的位置信息
//                mHandler.sendMessage(
//                    mHandler.obtainMessage(
//                        WHAT_UPDATE_PENDING_SEEK_POSITION,
//                        event
//                    )
//                )
//                plogv2(TAG) { "dispatchPendingSeek -> dispatch pending seek position update." }
//            }
//
//            if (event.action == KeyEvent.ACTION_UP && mHasDispatchPendingSeek) {
//                plogv2(TAG) { "dispatchPendingSeek -> key up and has dispatch pending seek, invoke it (currentPosition=${player.getCurrentPosition()} pendingSeekPosition=${pendingSeekPosition}." }
//                sendPendingSeekHandleMessage(event, -1)
//            }
//        }
//    }
//
//    /**
//     * @param delay 延迟时间
//     * @param removeUnHandledMessage 是否移除未处理的相同类型消息，默认移除
//     */
//    private fun sendPendingSeekHandleMessage(
//        event: KeyEvent,
//        delay: Long = -1,
//        removeUnHandledMessage: Boolean = true
//    ) {
//        if (removeUnHandledMessage) {
//            //先移除所有未处理的消息，确保handle消息只执行一次
//            mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
//        }
//        val msg = mHandler.obtainMessage(WHAT_HANDLE_PENDING_SEEK, event)
//        if (delay > 0) {
//            mHandler.sendMessageDelayed(msg, delay)
//        } else {
//            mHandler.sendMessage(msg)
//        }
//    }
//
//    override fun handlePendingSeek() {
//        super.handlePendingSeek()
//        mHasDispatchPendingSeek = false
//        mCurrentPendingSeekPosition = 0
//    }
//
//    override fun cancelPendingSeek() {
//        super.cancelPendingSeek()
//        mHasDispatchPendingSeek = false
//        mCurrentPendingSeekPosition = 0
//    }
//
//    /**
//     * 是否能够响应按键seek
//     */
//    private fun canPressKeyToSeek(): Boolean {
//        return isInPlaybackState && seekEnabled
//    }
//
//    abstract class PendingSeekCalculator {
//
//        /**
//         * 对外设置的用于控制的缩放系数
//         */
//        var seekRatio: Float = 1f
//
//        /**
//         * seek动作前做准备
//         * @param currentPosition 当前播放位置
//         * @param duration 总时间
//         * @param viewWidth 进度条宽度
//         */
//        abstract fun prepareCalculate(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        )
//
//        /**
//         * 返回本次seek的增量
//         */
//        abstract fun calculateIncrement(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ): Int
//
//        abstract fun reset()
//
//    }
//
//    class RatioStepSeekCalculator : PendingSeekCalculator() {
//
//        //默认步长 10秒
//        private var mStep = DEFAULT_STEP
//
//        companion object {
//            private const val DEFAULT_STEP = 5 * 1000
//            private const val MIN_STEP = 5 * 1000
//            private const val MAX_STEP = 90 * 1000
//        }
//
//        override fun prepareCalculate(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ) {
//            val step = duration / 100
//            mStep = max(MIN_STEP, step)
//            mStep = min(mStep, MAX_STEP)
//        }
//
//        override fun calculateIncrement(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ): Int {
//            //方向系数
//            val flag = if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
//            val eventTime = event.eventTime - event.downTime
//            val factor = 2.0.pow(eventTime / 1000.0)
//
//            return (mStep * flag * factor).toInt().also {
//                plogv2(TAG) {
//                    "calculateIncrement flag=$flag eventTime=$eventTime factor=$factor result=$it"
//                }
//            }
//        }
//
//        override fun reset() {
//            mStep = DEFAULT_STEP
//        }
//    }
//
//
//    class DurationSamplingSeekCalculator : PendingSeekCalculator() {
//
//        /**
//         * 增量最大倍数:相当于用户按住方向键一直做seek多少s之后达到最大的seek步长
//         */
//        private val maxIncrementFactor: Float = 16f
//
//        /**
//         * 最大的时间增量：默认为时长的百分之一，最小1000
//         */
//        private var maxIncrementTimeMs: Int = 0
//
//        /**
//         * 最小时间增量:最小1000
//         */
//        private var minIncrementTimeMs: Int = 0
//
//        /**
//         * 最少seek多少次seek完整个时长，默认500次，一次事件大概需要50毫秒，所以大致需要25s事件，也就是说一个很长的视频，最快25s seek完，但是由于是采用不断加速的形式，因此实际时间远大于25s
//         */
//        private val leastSeekCount = 400
//
//        override fun reset() {
//            //假设一个场景：设定两个变量 s = 面条的长度（很长很长）  c = 一个人最快吃多少口可以吃完。
//            // 假定1s时间内一个人能够吃 20口
//            //则一个人吃一口的最大长度 umax = s / c    假定一个系数f   这个人吃一口的最小长度 umin = umax / f
//            // 现在这个人从umin的速度开始吃，时间作为系数（不超过f），那么这个人吃完s需要多少时间？
//
//            //假定  s = 7200000 c = 500  f = 16
//            maxIncrementTimeMs = 0
//            minIncrementTimeMs = 0
//        }
//
//        override fun prepareCalculate(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ) {
//            maxIncrementTimeMs = duration / leastSeekCount
//            minIncrementTimeMs = (maxIncrementTimeMs / maxIncrementFactor).toInt()
//        }
//
//        override fun calculateIncrement(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ): Int {
//            //方向系数
//            val flag = if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
//            val eventTime = event.eventTime - event.downTime
//            val factor =
//                ceil(eventTime / 1000f).coerceAtMost(maxIncrementFactor) //时间转换成秒，作为系数,不超过最大的倍数
//            //本次偏移距离
//            return (factor * minIncrementTimeMs * seekRatio).toInt().coerceAtLeast(1000) * flag
//        }
//
//    }
//
//
//    override fun onControllerVisibilityChanged(isVisible: Boolean, anim: Animation?) {
//        super<VodControlComponent>.onControllerVisibilityChanged(isVisible, anim)
//        requestFocus()
//    }
//
//    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
//        TODO("Not yet implemented")
//    }
//
//    init {
//        descendantFocusability = FOCUS_AFTER_DESCENDANTS
//    }
//
    override fun onStartLeftOrRightKeyPressedForSeeking(event: KeyEvent) {
        mTrackingTouch = true
        setVodCtrlContainerVisibility(true, null)
    }

    override fun onStopLeftOrRightKeyPressedForSeeking(event: KeyEvent) {
        mTrackingTouch = false
        setVodCtrlContainerVisibility(false, null)
    }

    override fun onCancelLeftOrRightKeyPressedForSeeking(keyEvent: KeyEvent) {
        mTrackingTouch = false
        setVodCtrlContainerVisibility(false, null)
    }

    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        plogi2("KeyCtrl"){
            "slidePosition=$slidePosition ,currentPosition=$currentPosition ,duration=$duration"
        }
        setProgress(slidePosition, duration)
    }
}