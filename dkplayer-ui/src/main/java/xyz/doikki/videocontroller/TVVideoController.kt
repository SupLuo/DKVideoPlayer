package xyz.doikki.videocontroller

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import xyz.doikki.dkplayer.ui.UNDEFINED_LAYOUT
import xyz.doikki.videoplayer.TVCompatible
import xyz.doikki.videoplayer.controller.component.KeyControlComponent
import droid.unicstar.videoplayer.loopKeyWhen
import kotlin.math.ceil

@TVCompatible(message = "内部适配了在tv上拖动、播放完成重播、播放失败等逻辑")
open class TVVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : StandardVideoController(context, attrs, defStyleAttr, layoutId) {

    companion object {

        /**
         * 开始pending seek
         */
        private const val WHAT_BEGIN_PENDING_SEEK = 0x10

        /**
         * 取消pending seek
         */
        private const val WHAT_CANCEL_PENDING_SEEK = 0x11

        /**
         * 处理pending seek
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

    private val seekCalculator: PendingSeekCalculator = DurationSamplingSeekCalculator()

    /**
     * 是否处理KeyEvent
     */
    var keyEventEnable: Boolean = true

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
                        mControlComponents.loopKeyWhen<KeyControlComponent> {
                            it.onStartLeftOrRightKeyPressedForSeeking(event)
                        }
                        seekCalculator.prepareCalculate(event, currentPosition, duration, width)
                    }
                }
                WHAT_CANCEL_PENDING_SEEK -> {
                    cancelPendingSeek()
                    mControlComponents.loopKeyWhen<KeyControlComponent> {
                        it.onCancelLeftOrRightKeyPressedForSeeking(msg.obj as KeyEvent)
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
                        Log.d(
                            "TVController",
                            "action=${event.action}  eventTime=${event.eventTime - event.downTime} increment=${incrementTimeMs} previousPosition=${previousPosition} newPosition=${mCurrentPendingSeekPosition}"
                        )

                        /**
                         * 发送一个延迟消息，用于某些红外遥控器或者设备按键事件分发顺序不一致的问题：
                         * 即本身期望在[KeyEvent.ACTION_UP]的时候执行最终的seek动作，但是可能存在down事件还没有处理完的时候，系统已经接收了up事件，并且up事件没有下发到dispatchKeyEvent中
                         */
                        sendPendingSeekHandleMessage(event, 300)
                    }

                }
                WHAT_HANDLE_PENDING_SEEK -> {
                    val event = msg.obj as KeyEvent
                    //先做stop，再seek，避免loading指示器和seek指示器同时显示
                    mControlComponents.loopKeyWhen<KeyControlComponent> {
                        it.onStopLeftOrRightKeyPressedForSeeking(event)
                    }
                    handlePendingSeek()
                }
            }
        }
    }

    init {
        //设置可以获取焦点
        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = FOCUS_BEFORE_DESCENDANTS
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!keyEventEnable)
            return super.dispatchKeyEvent(event)

        Log.d(
            "dispatchKeyEvent",
            "keyCode = ${event.keyCode}   action = ${event.action} repeatCount = ${event.repeatCount} isInPlaybackState=${isInPlaybackState} " +
                    "isShowing=${isShowing} mHasDispatchPendingSeek=${mHasDispatchPendingSeek} mCurrentPendingSeekPosition=${mCurrentPendingSeekPosition}"
        )
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
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {//播放键
                if (uniqueDown && !isInPlaybackState) {//没有在播放中，则开始播放
                    invokeOnPlayerAttached(showToast = false) { player ->
                        player.start()
                    }
                    show()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {//暂停键
                if (uniqueDown && isInPlaybackState) {
                    invokeOnPlayerAttached(showToast = false) { player ->
                        player.pause()
                    }
                    show()
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CAMERA
            -> {//系统功能键
                // don't show the controls for volume adjustment
                //系统会显示对应的UI
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> {//左右键，做seek行为
                if (!canPressKeyToSeek()) {//不允许拖动
                    if (mHasDispatchPendingSeek) {
                        sendPendingSeekCancelMessage(event)
                    }
                    return true
                }
                if (uniqueDown && !isShowing) {
                    //第一次按下down并且当前控制器没有显示的情况下，只显示控制器
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
                    return true
                }
                handlePendingKeySeek(event)
                return true
            }
            else -> {
                show()
                return super.dispatchKeyEvent(event)
            }
        }
    }

    /**
     * 处理按键拖动
     */
    private fun handlePendingKeySeek(event: KeyEvent) {
        invokeOnPlayerAttached(showToast = false) { player ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (!mHasDispatchPendingSeek) {
                    mHasDispatchPendingSeek = true
                    sendBeginPendingSeekMessage(event)
                }
                sendPendingSeekPotionUpdateMessage(event)
            }

            if (event.action == KeyEvent.ACTION_UP && mHasDispatchPendingSeek) {
                Log.d(
                    "TVController",
                    "开始执行seek行为: pendingSeekPosition=${pendingSeekPosition}"
                )
                sendPendingSeekHandleMessage(event, -1)
            }
        }
    }

    private fun sendBeginPendingSeekMessage(event: KeyEvent) {
        mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)
        mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
        mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_BEGIN_PENDING_SEEK, event))
    }

    private fun sendPendingSeekPotionUpdateMessage(event: KeyEvent) {
        //更新pending seek的位置信息
        mHandler.sendMessage(
            mHandler.obtainMessage(
                WHAT_UPDATE_PENDING_SEEK_POSITION,
                event
            )
        )
    }

    private fun sendPendingSeekCancelMessage(event: KeyEvent) {
        mHandler.removeMessages(WHAT_UPDATE_PENDING_SEEK_POSITION)
        mHandler.removeMessages(WHAT_HANDLE_PENDING_SEEK)
        mHandler.removeMessages(WHAT_CANCEL_PENDING_SEEK)
        mHandler.sendMessage(mHandler.obtainMessage(WHAT_CANCEL_PENDING_SEEK, event))
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
    }

    override fun cancelPendingSeek() {
        super.cancelPendingSeek()
        mHasDispatchPendingSeek = false
        mCurrentPendingSeekPosition = 0
    }

    /**
     * 是否能够响应按键seek
     */
    private fun canPressKeyToSeek(): Boolean {
        return isInPlaybackState && seekEnabled
    }


    abstract class PendingSeekCalculator {

        /**
         * 对外设置的用于控制的缩放系数
         */
        var seekRatio: Float = 1f

        /**
         * seek动作前做准备
         */
        abstract fun prepareCalculate(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        )

        /**
         * 返回本次seek的增量
         */
        abstract fun calculateIncrement(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        ): Int

        abstract fun reset()

    }


    class DurationSamplingSeekCalculator : PendingSeekCalculator() {

        /**
         * 增量最大倍数:相当于用户按住方向键一直做seek多少s之后达到最大的seek步长
         */
        private val maxIncrementFactor: Float = 16f

        /**
         * 最大的时间增量：默认为时长的百分之一，最小1000
         */
        private var maxIncrementTimeMs: Int = 0

        /**
         * 最小时间增量:最小1000
         */
        private var minIncrementTimeMs: Int = 0

        /**
         * 最少seek多少次seek完整个时长，默认500次，一次事件大概需要50毫秒，所以大致需要25s事件，也就是说一个很长的视频，最快25s seek完，但是由于是采用不断加速的形式，因此实际时间远大于25s
         */
        private val leastSeekCount = 400

        override fun reset() {
            //假设一个场景：设定两个变量 s = 面条的长度（很长很长）  c = 一个人最快吃多少口可以吃完。
            // 假定1s时间内一个人能够吃 20口
            //则一个人吃一口的最大长度 umax = s / c    假定一个系数f   这个人吃一口的最小长度 umin = umax / f
            // 现在这个人从umin的速度开始吃，时间作为系数（不超过f），那么这个人吃完s需要多少时间？

            //假定  s = 7200000 c = 500  f = 16
            maxIncrementTimeMs = 0
            minIncrementTimeMs = 0
        }

        override fun prepareCalculate(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        ) {
            maxIncrementTimeMs = duration / leastSeekCount
            minIncrementTimeMs = (maxIncrementTimeMs / maxIncrementFactor).toInt()
        }

        override fun calculateIncrement(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        ): Int {
            //方向系数
            val flag = if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
            val eventTime = event.eventTime - event.downTime
            val factor =
                ceil(eventTime / 1000f).coerceAtMost(maxIncrementFactor) //时间转换成秒，作为系数,不超过最大的倍数
            //本次偏移距离
            return (factor * minIncrementTimeMs * seekRatio).toInt().coerceAtLeast(1000) * flag
        }

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
}