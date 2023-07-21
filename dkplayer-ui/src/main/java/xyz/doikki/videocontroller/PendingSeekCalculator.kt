package xyz.doikki.videocontroller

import android.view.KeyEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import unics.player.internal.plogv2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

sealed class PendingSeekCalculator {

    /**
     * seek动作前做准备
     * @param currentPosition 当前播放位置
     * @param duration 总时间
     * @param viewWidth 进度条宽度
     */
    open fun prepareCalculate(
        event: KeyEvent,
        currentPosition: Int,
        duration: Int,
        viewWidth: Int
    ) {
    }

    /**
     * 返回本次seek的增量
     */
    abstract fun calculateIncrement(
        event: KeyEvent,
        currentPosition: Int,
        duration: Int,
        viewWidth: Int
    ): Int

    open fun reset() {}

    companion object {

        @JvmStatic
        fun linearStepCalculator(step: Int = 15 * 1000): PendingSeekCalculator {
            return LinearPendingSeekCalculator(step)
        }

        @JvmStatic
        fun dynamicAccelerateCalculator(): PendingSeekCalculator {
            return DynamicAcceleratePendingSeekCalculator()
        }

        @JvmStatic
        fun accelerateCalculator(
            minStep: Int = 8 * 1000,
            maxStep: Int = 90 * 1000,
            accelerateTime: Float = 5 * 1000f,
            onceStep: Int = 10 * 1000,
        ): PendingSeekCalculator {
            return AcceleratePendingSeekCalculator(minStep, maxStep, accelerateTime, onceStep)
        }
    }

    /**
     * 线性步长：即每次seek的距离都是相同的
     * @param mStep 步长，默认8s
     */
    internal class LinearPendingSeekCalculator(private val mStep: Int) :
        PendingSeekCalculator() {

        override fun calculateIncrement(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        ): Int {
            val flag = if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
            return (flag * mStep).also {
                plogv2("PendingSeekCalculator") {
                    "[LinearPendingSeekCalculator] calculateIncrement : flag=$flag eventTime=${event.eventTime} result=$it"
                }
            }
        }

    }

    /**
     * 加速Seek计算器，会以[AccelerateDecelerateInterpolator]作为加速方式，根据按键按下的持续时间，从[minStep]逐渐增加到[maxStep]
     *
     * @param minStep 单次seek的最小速度，即从该值开始逐渐增加
     * @param maxStep 单次seek的最大速度，即加速达到的最大速度
     * @param accelerateTime 加速的时间，即经过多少时间后达到最大值
     * @param onceStep 只seek一次的步长：即用户只按下一次方向键
     */
    private class AcceleratePendingSeekCalculator(
        private val minStep: Int,
        private val maxStep: Int,
        private val accelerateTime: Float = 5 * 1000f,
        private val onceStep: Int = 10 * 1000,
    ) : PendingSeekCalculator() {

        private val mInterceptor: Interpolator = AccelerateDecelerateInterpolator()
        private val mStepDiff: Int

        init {
            require(maxStep > minStep) {
                "min step must be smaller than max step."
            }
            mStepDiff = maxStep - minStep
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
            val increment = if (event.repeatCount == 0) {
                flag * onceStep
            } else {
                val fraction = if (eventTime > accelerateTime) {
                    1f
                } else if (eventTime <= 0) {
                    0f
                } else {
                    eventTime / accelerateTime
                }
                (minStep + mStepDiff * mInterceptor.getInterpolation(fraction)).toInt() * flag
            }
            plogv2("PendingSeekCalculator") {
                "[AcceleratePendingSeekCalculator] calculateIncrement : flag=$flag eventTime=$eventTime increment=$increment"
            }
            return increment
        }

    }

    /**
     * 动态加速步长：即最小值和最大值会根据视频持续时间动态调整
     * @param onceStep 只seek一次的步长：即用户只按下一次方向键
     */
    private class DynamicAcceleratePendingSeekCalculator(private val onceStep: Int = 10 * 1000) : PendingSeekCalculator() {

        companion object {
            const val DEF_MIN_STEP_BASE = 3 * 1000
            const val DEF_MAX_STEP_BASE = 30 * 1000
            const val DEF_ACCELERATE_TIME = 4 * 1000f
        }

        private val mInterceptor: Interpolator = AccelerateDecelerateInterpolator()

        private var mMinStep: Int = DEF_MIN_STEP_BASE
        private var mMaxStep: Int = DEF_MAX_STEP_BASE
        private var mAccelerateTime: Float = DEF_ACCELERATE_TIME

        private var mStepDiff: Int = mMaxStep - mMinStep

        override fun prepareCalculate(
            event: KeyEvent,
            currentPosition: Int,
            duration: Int,
            viewWidth: Int
        ) {
            val scale = if (duration < 20 * 60 * 1000) {
                1
            } else if (duration < 40 * 60 * 1000) {
                2
            } else {
                3
            }
            mMinStep = DEF_MIN_STEP_BASE * scale
            mMaxStep = DEF_MAX_STEP_BASE * scale
            mStepDiff = mMaxStep - mMinStep
            mAccelerateTime = DEF_ACCELERATE_TIME + scale * 1000
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
            val increment = if (event.repeatCount == 0) {
                flag * onceStep
            } else {
                val fraction = if (eventTime > mAccelerateTime) {
                    1f
                } else if (eventTime <= 0) {
                    0f
                } else {
                    eventTime / mAccelerateTime
                }
                (mMinStep + mStepDiff * mInterceptor.getInterpolation(fraction)).toInt() * flag
            }
            plogv2("PendingSeekCalculator") {
                "[DynamicAcceleratePendingSeekCalculator] calculateIncrement : flag=$flag eventTime=$eventTime increment=$increment"
            }
            return increment
        }

        override fun reset() {
            mMinStep = DEF_MIN_STEP_BASE
            mMaxStep = DEF_MAX_STEP_BASE
            mAccelerateTime = DEF_ACCELERATE_TIME
            mStepDiff = mMaxStep - mMinStep
        }

    }

}