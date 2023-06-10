//package unics.player.control.widget
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.Rect
//import android.graphics.RectF
//import android.graphics.drawable.Drawable
//import android.util.AttributeSet
//import android.util.Log
//import android.view.View
//import androidx.annotation.ColorInt
//import androidx.annotation.Px
//import unics.player.internal.plogw2
//import kotlin.math.max
//
//
//class FloatStatableSeekBar @JvmOverloads constructor(
//    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private val TAG = "[FloatStatableSeekBar@${this.hashCode()}]"
//
//    companion object {
//
//
//        /*圆角*/
//        const val ROUNDED_CORNERS = 0
//
//        /*方形*/
//        const val SQUARE_CORNERS = 1
//    }
//
//    data class Configs(
//        /**
//         * 是否是裁剪模式：如果为裁剪模式，则thumb在bar的两端会出现被裁剪的显示不全（如果parent 设置clip=false，则也能看到），如果未非裁剪模式，则本view会在左右预留thumb被裁剪的空间以便绘制完全（直观感受就是左右会多一些空白区域）
//         */
//        @JvmField
//        internal var clipMode: Boolean = false,
//
//
//        @JvmField
//        internal var progress: Int = 0,
//
//        @JvmField
//        internal var secondaryProgress: Int = 0,
//
//        @Px
//        @JvmField
//        internal var barHeight: Int = 3,
//
//        @JvmField
//        internal var cornerStyle: Int = ROUNDED_CORNERS
//    )
//
//    @JvmField
//    internal var mMin: Int = 0
//
//    @JvmField
//    internal var mMax: Int = 100
//
//    private lateinit var mUiState: Configs
//
//    /*一级进度值*/
//    private var mProgress: Int = 0
//
//    /*二级进度值*/
//    private var mSecondaryProgress: Int = 0
//
//    /*bar背景色*/
//    private var mBarColor: Int = 0
//
//    /*一级进度颜色*/
//    private var mProgressColor: Int = 0
//
//    /*二级进度颜色*/
//    private var mSecondaryProgressColor: Int = 0
//
//    /**
//     * 可绘制区域
//     */
//    private var mDrawRect = Rect()
//
//    /*进度条画笔*/
//    private val mBarPaint = Paint().also {
////        it.isAntiAlias = true //抗锯齿功能，会消耗较大资源，绘制图形速度会变慢
////        it.isDither = true //抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
//    }
//    private val mTmpRectF = RectF()
//    private val mTmpRect = Rect()
//
//    private var mThumb: Drawable? = null
//
//    fun setProgress(progress: Int, animate: Boolean) {
//        setProgressInternal(progress, false, animate)
//    }
//
//    @Synchronized
//    internal fun setProgressInternal(progress: Int, fromUser: Boolean, animate: Boolean): Boolean {
//        var tmpProgress = progress
//
//        tmpProgress = constrain(tmpProgress, mMin, mMax)
//        if (tmpProgress == mProgress) {
//            // No change from current.
//            return false
//        }
//        mProgress = tmpProgress
//        refreshProgress(R.id.progress, mProgress, fromUser, animate)
//        return true
//    }
//
//    @Synchronized
//    fun setSecondaryProgress(secondaryProgress: Int) {
//        var secondaryProgress = secondaryProgress
//
//        if (secondaryProgress < mMin) {
//            secondaryProgress = mMin
//        }
//        if (secondaryProgress > mMax) {
//            secondaryProgress = mMax
//        }
//        if (secondaryProgress != mSecondaryProgress) {
//            mSecondaryProgress = secondaryProgress
//            refreshProgress(R.id.secondaryProgress, mSecondaryProgress, false, false)
//        }
//    }
//
//    @Synchronized
//    fun getProgress(): Int {
//        return mProgress
//    }
//
//    @Synchronized
//    fun getSecondaryProgress(): Int {
//        return mSecondaryProgress
//    }
//
//    @Synchronized
//    fun getMin(): Int {
//        return mMin
//    }
//
//    @Synchronized
//    fun getMax(): Int {
//        return mMax
//    }
//
//    @Synchronized
//    fun setMin(min: Int) {
//        var tmpMin = min
//        if (mMaxInitialized) {
//            if (tmpMin > mMax) {
//                tmpMin = mMax
//            }
//        }
//        mMinInitialized = true
//        if (mMaxInitialized && tmpMin != mMin) {
//            mMin = tmpMin
//            postInvalidate()
//            if (mProgress < tmpMin) {
//                mProgress = tmpMin
//            }
//            refreshProgress(R.id.progress, mProgress, false, false)
//        } else {
//            mMin = tmpMin
//        }
//    }
//
//    @Synchronized
//    fun setMax(max: Int) {
//        var max = max
//        if (mMinInitialized) {
//            if (max < mMin) {
//                max = mMin
//            }
//        }
//        mMaxInitialized = true
//        if (mMinInitialized && max != mMax) {
//            mMax = max
//            postInvalidate()
//            if (mProgress > max) {
//                mProgress = max
//            }
//            refreshProgress(R.id.progress, mProgress, false, false)
//        } else {
//            mMax = max
//        }
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val dh = paddingTop + max(mUiState.barHeight, mThumb?.intrinsicHeight ?: 0) + paddingBottom
//        setMeasuredDimension(
//            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
//            resolveSizeAndState(dh, heightMeasureSpec, 0)
//        )
//    }
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        mDrawRect.set(
//            left + paddingLeft,
//            top + paddingTop,
//            right - paddingRight,
//            bottom - paddingBottom
//        )
//    }
//
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        drawProgressBar(canvas)
//        drawIndicator(canvas)
//    }
//
//    private val barOffset: Float
//        get() = if (mUiState.clipMode) 0f else (mThumb?.intrinsicWidth ?: 0) / 2f
//
//    private fun drawIndicator(canvas: Canvas) {
//        val thumb = mThumb ?: return
//        val hOffset = barOffset
//        val left = mDrawRect.left + barOffset
//        val right = mDrawRect.right - barOffset
//        val x = left + calculateBarProgressX(mProgress, right - left)
//        val y = mDrawRect.exactCenterY()
//
//        val thumbHalfWidth = thumb.intrinsicWidth / 2
//        val thumbHalfHeight = thumb.intrinsicHeight / 2
//        mTmpRect.set(
//            (x - thumbHalfWidth).toInt(),
//            (y - thumbHalfHeight).toInt(),
//            (x + thumbHalfWidth).toInt(),
//            (y + thumbHalfHeight).toInt()
//        )
//
//        thumb.bounds = mTmpRect
//        thumb.draw(canvas)
//
//        if (mDrawSecondaryIndicator) {
//            if (mSecondaryIndicatorScale > 0) {
//                //计算缩放之后的大小
//                val size = mSecondaryIndicatorSize * mSecondaryIndicatorScale
//                val half = size / 2
//                mSecondaryIndicatorRect.set(
//                    (x - half).toInt(), (y - half).toInt(), (x + half).toInt(),
//                    (y + half).toInt()
//                )
//                Log.d(
//                    "IndicatorDrawable",
//                    "scale=${mSecondaryIndicatorScale} with=${mSecondaryIndicatorRect.width()} height=${mSecondaryIndicatorRect.height()}"
//                )
//                Log.d(
//                    "IndicatorDrawable",
//                    "left=${mSecondaryIndicatorRect.left} ${mSecondaryIndicatorRect.top} ${mSecondaryIndicatorRect.right} ${mSecondaryIndicatorRect.bottom}"
//                )
//
//            }
//        } else {
//            mBarPaint.color = mIndicatorColor
//            //外圆
//            canvas.drawCircle(x, y, mIndicatorRadius, mBarPaint)
//
//            //内圆
//            mBarPaint.color = mIndicatorInnerColor
//            canvas.drawCircle(
//                x,
//                y,
//                mIndicatorInnerRadius.toFloat(),
//                mBarPaint
//            )
//        }
//
//
//    }
//
//    /**
//     * 绘制的bar在左右留出了半个indicator指示器的宽度，因此其他地方计算要注意这一点
//     */
//    private fun drawProgressBar(canvas: Canvas) {
//        val isRounded = mUiState.cornerStyle == ROUNDED_CORNERS
//        val halfBarSize = mUiState.barHeight / 2f
//        val hOffset = barOffset
//        val left = mDrawRect.left + hOffset
//        val right = mDrawRect.right - hOffset
//        val centerY = mDrawRect.exactCenterY()
//        val top = centerY - halfBarSize
//        val bottom = centerY + halfBarSize
//
//        //绘制背景
//        drawBarImpl(canvas, mMax, left, top, right, bottom, mBarColor, isRounded)
//        //二级进度
//        if (mSecondaryProgress > 0 && mSecondaryProgress > mProgress) {
//            drawBarImpl(
//                canvas,
//                mSecondaryProgress,
//                left,
//                top,
//                right,
//                bottom,
//                mSecondaryProgressColor,
//                isRounded
//            )
//        }
//        //一级进度
//        if (mProgress > 0) {
//            drawBarImpl(canvas, mProgress, left, top, right, bottom, mProgressColor, isRounded)
//        }
//    }
//
//    private fun drawBarImpl(
//        canvas: Canvas,
//        progress: Int,
//        left: Float,
//        top: Float,
//        right: Float,
//        bottom: Float,
//        @ColorInt color: Int,
//        isRounded: Boolean
//    ) {
//        mBarPaint.color = color
//        val newRight = left + calculateBarProgressX(progress, right - left)
//        mTmpRectF.set(left, top, newRight, bottom)
//        if (isRounded) {
//            val radius = (bottom - top) / 2f
//            canvas.drawRoundRect(mTmpRectF, radius, radius, mBarPaint)
//        } else {
//            canvas.drawRect(mTmpRectF, mBarPaint)
//        }
//    }
//
//    /**
//     * 根据进度计算进度条x坐标
//     */
//    private fun calculateBarProgressX(progress: Int, width: Float): Float {
//        if (progress == mMax)
//            return width
//        return width * (progress - mMin) / (mMax - mMin).toFloat()
//    }
//
//    private fun constrain(value: Int, min: Int, max: Int): Int {
//        if (value < min) {
//            plogw2(TAG) { "progress value(=$value) is smaller than min value(=$max)." }
//            return min
//        }
//
//        if (value > max) {
//            plogw2(TAG) { "progress value(=$value) is greater than max value(=$max)." }
//            return max
//        }
//
//        return value
//    }
//}