package xyz.doikki.videocontroller

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.LayoutRes
import unics.player.control.TVCompatible
import unics.player.controller.KeyControlComponent
import unics.player.controller.StandardVideoController
import unics.player.internal.plogv2
import xyz.doikki.dkplayer.ui.UNDEFINED_LAYOUT
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@TVCompatible(message = "内部适配了在tv上拖动、播放完成重播、播放失败等逻辑")
open class TVVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : StandardVideoController(context, attrs, layoutId) {

    companion object {

        private const val TAG = "TVController"

        /**
         * 开始pending seek
         */
        private const val WHAT_BEGIN_PENDING_SEEK = 0x10

        /**
         * 取消pending seek
         */
        private const val WHAT_CANCEL_PENDING_SEEK = 0x11

        /**
         * 执行pending seek
         */
        private const val WHAT_HANDLE_PENDING_SEEK = 0x12

        /**
         * 更新pending seek的位置
         */
        private const val WHAT_UPDATE_PENDING_SEEK_POSITION = 0x13
    }

    /**
     * 是否已经触发过pending seek的意图
     */
    private var mHasDispatchPendingSeek: Boolean = false

    /**
     * 当前待seek的位置
     */
    private var mCurrentPendingSeekPosition: Int = 0

    private var seekCalculator: PendingSeekCalculator =
        PendingSeekCalculator.dynamicAccelerateCalculator()

    /**
     * 是否处理KeyEvent
     */
    var keyEventEnable: Boolean = true

    /**
     * 是否是直播
     */
    var isLive: Boolean = false

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                WHAT_BEGIN_PENDING_SEEK -> {
                    invokeOnPlayerAttached { player ->
                        val duration = player.getDuration().toInt()
                        val currentPosition = player.getCurrentPosition().toInt()
                        val event = msg.obj as KeyEvent
                        mCurrentPendingSeekPosition = currentPosition
                        for ((key) in mControlComponents) {
                            if (key is KeyControlComponent) {
                                key.onStartLeftOrRightKeyPressedForSeeking(event)
                            }
                        }
                        seekCalculator.prepareCalculate(event, currentPosition, duration, width)
                        stopFadeOut()
                    }
                }

                WHAT_CANCEL_PENDING_SEEK -> {
                    cancelPendingSeek()
                    for ((key) in mControlComponents) {
                        if (key is KeyControlComponent) {
                            key.onCancelLeftOrRightKeyPressedForSeeking(msg.obj as KeyEvent)
                        }
                    }
                }

                WHAT_UPDATE_PENDING_SEEK_POSITION -> {
                    invokeOnPlayerAttached { player ->
                        val duration = player.getDuration().toInt()
                        val event = msg.obj as KeyEvent
                        val previousPosition = mCurrentPendingSeekPosition
                        val incrementTimeMs =
                            seekCalculator.calculateIncrement(
                                event,
                                previousPosition,
                                duration,
                                width
                            )
                        mCurrentPendingSeekPosition =
                            (mCurrentPendingSeekPosition + incrementTimeMs)
                                .coerceAtLeast(0)
                                .coerceAtMost(duration)

                        setPendingSeekPositionAndNotify(
                            mCurrentPendingSeekPosition,
                            previousPosition,
                            duration
                        )
                        plogv2(TAG) {
                            "update pending seek : action=${event.action}  eventTime=${event.eventTime - event.downTime} increment=${incrementTimeMs} previousPosition=${previousPosition} newPosition=${mCurrentPendingSeekPosition}"
                        }
                        /**
                         * 发送一个延迟消息，用于某些红外遥控器或者设备按键事件分发顺序不一致的问题：
                         * 即本身期望在[KeyEvent.ACTION_UP]的时候执行最终的seek动作，但是可能存在down事件还没有处理完的时候，系统已经接收了up事件，并且up事件没有下发到dispatchKeyEvent中
                         */
                        sendPendingSeekHandleMessage(event, 1500)
                    }

                }

                WHAT_HANDLE_PENDING_SEEK -> {
                    val event = msg.obj as KeyEvent
                    //先做stop，再seek，避免loading指示器和seek指示器同时显示
                    for ((key) in mControlComponents) {
                        if (key is KeyControlComponent) {
                            key.onStopLeftOrRightKeyPressedForSeeking(event)
                        }
                    }
                    handlePendingSeek()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = super.dispatchKeyEvent(event)
        if (handled || !keyEventEnable)
            return handled

        plogv2(TAG) {
            "dispatchKeyEvent -> keyCode = ${event.keyCode}   action = ${event.action} repeatCount = ${event.repeatCount} isInPlaybackState=${isInPlaybackState} " +
                    "isShowing=${isShowing} mHasDispatchPendingSeek=${mHasDispatchPendingSeek} mCurrentPendingSeekPosition=${mCurrentPendingSeekPosition}"
        }
        val keyCode = event.keyCode
        val uniqueDown = (event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN)
        when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {//返回键&菜单键逻辑
                if (uniqueDown && isShowing) {
                    //如果当前显示了控制器，则隐藏；
                    hide()
                    return true
                }
                return false
            }

            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER
            -> {//播放/暂停切换键
                if (isLive) {
                    if (uniqueDown) {//没有在播放中，则开始播放
                        invokeOnPlayerAttached(showToast = false) {
                            if (isInCompleteState) {
                                replay(resetPosition = true)
                            } else if (isInErrorState) {
                                replay(resetPosition = false)
                            }
                        }
                        show()
                        return true
                    }
                } else {
                    if (uniqueDown) {  //第一次按下Ok键/播放暂停键/空格键
                        if (isInPlaybackState) {
                            //正在播放过程中，则切换播放
                            togglePlay()
                        } else if (isInCompleteState) {
                            replay(resetPosition = true)
                        } else if (isInErrorState) {
                            replay(resetPosition = false)
                        }
                        show()
                        return true
                    }
                }
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {//播放键
                if (uniqueDown && !isInPlaybackState) {//没有在播放中，则开始播放
                    invokeOnPlayerAttached(showToast = false) { player ->
                        player.start()
                    }
                    show()
                    return true
                }
            }

            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {//暂停键
                if (!isLive && uniqueDown && isInPlaybackState) {
                    invokeOnPlayerAttached(showToast = false) { player ->
                        player.pause()
                    }
                    show()
                    return true
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CAMERA
            -> {//系统功能键
                // don't show the controls for volume adjustment
                //系统会显示对应的UI
            }

            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> {//左右键，做seek行为

                if (isLive) {
                    plogv2(TAG) { "is live mode ,not response dpad left and right key event." }
                    return handled
                }

                plogv2(TAG) { "handle dpad right or left key for pending seek." }
                if (!(seekEnabled && isInPlaybackState)) {//不允许拖动
                    plogv2(TAG) { "pending seek disabled." }
                    if (mHasDispatchPendingSeek) {
                        mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)
                        mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
                        mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
                        mHandler.sendMessage(
                            mHandler.obtainMessage(
                                WHAT_CANCEL_PENDING_SEEK,
                                event
                            )
                        )
                        plogv2(TAG) { "has dispatched pending seek,cancel it." }
                        mHasDispatchPendingSeek = false
                    }
                    return handled
                }
                if (uniqueDown && !isShowing) {
                    //第一次按下down并且当前控制器没有显示的情况下，只显示控制器
                    plogv2(TAG) { "first pressed left/right dpad and controller is not showing,show it only." }
                    show()
                    return true
                }
                //后续的逻辑存在以下几种情况：
                //1、第一次按下down，并且控制已经显示，此时应该做seek动作
                //2、执行up（存在可能已经有seek动作，或者没有seek动作：即按下down之后，立马执行了up）
                //3、第N次按下down（n >1 ）

                if (event.action == KeyEvent.ACTION_UP && !mHasDispatchPendingSeek) {
                    //按下down之后执行了up，相当于只按了一次方向键，
                    // 并且没有执行过pending行为（即单次按键的时候控制器还未显示，控制器已经显示的情况下单次按键是有效的行为），不做seek动作
                    plogv2(TAG) { "left/right dpad key up, but not dispatch pending seek,return directly." }
                    show()
                    return true
                }
                dispatchPendingSeek(event)
                return true
            }

            else -> {
                show()
            }
        }
        return handled
    }

    /**
     * 处理按键拖动
     */
    private fun dispatchPendingSeek(event: KeyEvent) {
        plogv2(TAG) { "dispatchPendingSeek" }
        invokeOnPlayerAttached(showToast = false) { player ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (!mHasDispatchPendingSeek) {
                    mHasDispatchPendingSeek = true
                    mHandler.removeMessages(WHAT_BEGIN_PENDING_SEEK)
                    mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
                    mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
                    mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)

                    mHandler.sendMessage(mHandler.obtainMessage(WHAT_BEGIN_PENDING_SEEK, event))
                    plogv2(TAG) { "dispatchPendingSeek -> dispatch pending seek start." }
                }
                //更新pending seek的位置信息
                mHandler.sendMessage(
                    mHandler.obtainMessage(
                        WHAT_UPDATE_PENDING_SEEK_POSITION,
                        event
                    )
                )
                plogv2(TAG) { "dispatchPendingSeek -> dispatch pending seek position update." }
            }

            if (event.action == KeyEvent.ACTION_UP && mHasDispatchPendingSeek) {
                plogv2(TAG) { "dispatchPendingSeek -> key up and has dispatch pending seek, invoke it (currentPosition=${player.getCurrentPosition()} pendingSeekPosition=${pendingSeekPosition}." }
                sendPendingSeekHandleMessage(event, -1)
            }
        }
    }

    /**
     * @param delay 延迟时间
     * @param removeUnHandledMessage 是否移除未处理的相同类型消息，默认移除
     */
    private fun sendPendingSeekHandleMessage(
        event: KeyEvent,
        delay: Long = -1,
        removeUnHandledMessage: Boolean = true
    ) {
        if (removeUnHandledMessage) {
            //先移除所有未处理的消息，确保handle消息只执行一次
            mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
        }
        val msg = mHandler.obtainMessage(WHAT_HANDLE_PENDING_SEEK, event)
        if (delay > 0) {
            mHandler.sendMessageDelayed(msg, delay)
        } else {
            mHandler.sendMessage(msg)
        }
    }

    override fun handlePendingSeek() {
        super.handlePendingSeek()
        mHasDispatchPendingSeek = false
        mCurrentPendingSeekPosition = 0
        startFadeOut()
    }

    override fun cancelPendingSeek() {
        super.cancelPendingSeek()
        mHasDispatchPendingSeek = false
        mCurrentPendingSeekPosition = 0
        startFadeOut()
    }

    /**
     * 是否能够响应按键seek
     */
    private fun canPressKeyToSeek(): Boolean {
        return isInPlaybackState && seekEnabled
    }


//    class BasedOnWidthSeekCalculator : PendingSeekCalculator() {
//
//        /**
//         * 每次按键偏移的距离
//         */
//        val deltaPixelsStep = 4f
//
//        /**
//         * 最大倍数
//         */
//        val maxDeltaPixelsRatio: Float = 16f
//
//        /**
//         * 每次偏移的最小时间ms
//         */
//        val minOffsetTimeMs = 1000
//
//        override fun prepareCalculate(
//            event: KeyEvent,
//            currentPosition: Int,
//            duration: Int,
//            viewWidth: Int
//        ) {
//
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
//
//
//            val eventTime = event.eventTime - event.downTime
//            val scale = ceil(eventTime / 1000f).coerceAtMost(maxDeltaPixelsRatio) //时间转换成秒，作为系数
//
//            //本次偏移距离
//            val incrementOffset =
//                ().coerceAtMost(maxIncrementDeltaPixels)
//
//            //本次增加的偏移时间 至少minOffsetTimeMs
//            val incrementTimeMs =
//                (scale * deltaPixelsStep / viewWidth * seekRatio * duration).toInt()
//                    .coerceAtLeast(minOffsetTimeMs) * flag
//        }
//
//
//    }

    init {
        //设置可以获取焦点
        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = FOCUS_BEFORE_DESCENDANTS
    }
}