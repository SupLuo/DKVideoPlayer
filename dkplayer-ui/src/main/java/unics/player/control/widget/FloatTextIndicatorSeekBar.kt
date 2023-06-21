package unics.player.control.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import unics.player.control.dip
import unics.player.control.orDefault
import unics.player.control.sp
import unics.player.control.use
import xyz.doikki.videocontroller.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class FloatTextIndicatorSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_PATTERN = "HH:mm:ss"
        const val ENDPOINT_MODE_OFFSET_TEXT = 0
        const val ENDPOINT_MODE_OFFSET_SEEKBAR = 1
        const val ENDPOINT_MODE_CLIP_TEXT = 2
    }

    private val mSeekBar: SeekBar
    private val mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        //设置水平居中展示
        it.textAlign = Paint.Align.CENTER
    }
    private lateinit var mTextFontMetricsInt: Paint.FontMetricsInt
    private var mText: String = "00:00:00"
    private val mTextRect: Rect = Rect()
    private var mTextOffset: Int = context.dip(4).toInt()
    private val mDateFormat: DateFormat
    private val mDate: Date = Date()
    private val mTextColor: ColorStateList
    private var mEndpointMode: Int = ENDPOINT_MODE_OFFSET_TEXT

    //seekbar额外偏移的距离
    private var mSeekBarExtraOffsetH: Int = 0

    /**
     * 设置当前时间
     */
    fun setTime(time: Long) {
        mDate.time = time
        setText(mDateFormat.format(mDate))
        androidx.appcompat.R.styleable.AppCompatSeekBar
    }

    fun setText(text: String) {
        mText = text
        invalidate()
    }

    fun setProgress(progress: Int) {
        if (mSeekBar.progress == progress)
            return
        mSeekBar.progress = progress
//        //重绘自己
//        invalidate()
    }

    fun setProgress(progress: Int, animate: Boolean) {
        if (mSeekBar.progress == progress)
            return

        if (Build.VERSION.SDK_INT >= 24)
            mSeekBar.setProgress(progress, animate)
        else
            setProgress(progress)
    }

    fun setSecondaryProgress(secondaryProgress: Int) {
        if (mSeekBar.secondaryProgress == secondaryProgress)
            return
        mSeekBar.secondaryProgress = secondaryProgress
    }

    fun getProgress(): Int {
        return mSeekBar.progress
    }

    fun getSecondaryProgress(): Int {
        return mSeekBar.secondaryProgress
    }

    fun getMax(): Int {
        return mSeekBar.max
    }

    fun setMax(max: Int) {
        if (mSeekBar.max == max)
            return
        mSeekBar.max = max
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!::mTextFontMetricsInt.isInitialized) {
            setup()
        }
        val textHeight = mTextFontMetricsInt.bottom - mTextFontMetricsInt.top
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child == mSeekBar) {
                measureChildWithMargins(
                    child,
                    widthMeasureSpec,
                    mSeekBarExtraOffsetH * 2,//左右预留的空间，避免文本显示不全
                    heightMeasureSpec,
                    textHeight + mTextOffset
                )
            }
        }
        val lp = mSeekBar.layoutParams as MarginLayoutParams
        setMeasuredDimension(
            paddingLeft + mSeekBarExtraOffsetH + lp.leftMargin + mSeekBar.measuredWidth + lp.rightMargin + mSeekBarExtraOffsetH + paddingRight,
            paddingTop + textHeight + mTextOffset + lp.topMargin + mSeekBar.measuredHeight + lp.bottomMargin + paddingBottom
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val lp = mSeekBar.layoutParams as MarginLayoutParams
        val childLeft = paddingLeft + lp.leftMargin + mSeekBarExtraOffsetH
        val childRight = (r - l) - paddingRight - lp.rightMargin - mSeekBarExtraOffsetH
        val childBottom = (b - t) - paddingBottom - lp.bottomMargin
        mSeekBar.layout(
            childLeft,
            (childBottom - mSeekBar.measuredHeight).coerceAtLeast(paddingTop),
            childRight,
            childBottom
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawText(canvas)
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//    }

    private fun drawText(canvas: Canvas) {
        val text = mText
        if (text.isEmpty())
            return
        val textCenterX = getThumbCenterX(getScale()) + mSeekBar.left
        //测量文本大小
        mTextPaint.getTextBounds(text, 0, text.length, mTextRect)
        val halfTextW = mTextRect.width() / 2
        val x = when (mEndpointMode) {
            ENDPOINT_MODE_OFFSET_SEEKBAR, ENDPOINT_MODE_OFFSET_TEXT -> {
                //考虑文字显示不全的情况
                textCenterX
                    .coerceAtLeast(paddingLeft + halfTextW)//不超过左边界
                    .coerceAtMost(width - paddingRight - halfTextW)//不超过右边界
            }
            else -> {
                //不用考虑文字显示不全
                textCenterX
            }
        }
        val fm = mTextFontMetricsInt
        //绘制文字
        val distance = (fm.descent - fm.ascent) / 2f - fm.bottom
        mTextRect.centerY()
        val baseline: Float =
            (height - paddingTop - paddingBottom - mSeekBar.height - mTextOffset) / 2 + distance
        canvas.drawText(text, x.toFloat(), baseline, mTextPaint)
    }

    /**
     * 获取thumb在seekbar中的中心位置，主要参考[android.widget.AbsSeekBar#setThumbPos]
     */
    private fun getThumbCenterX(scale: Float): Int {
        var available: Int = mSeekBar.width - mSeekBar.paddingLeft - mSeekBar.paddingRight
        val thumbWidth: Int = mSeekBar.thumb?.intrinsicWidth.orDefault()
        available -= thumbWidth
        available += mSeekBar.thumbOffset * 2

        val thumbPos: Int = (scale * available + 0.5f).toInt()

        val left = if (isLayoutRtl()) available - thumbPos else thumbPos
        val right = left + thumbWidth
        return ((right - left) / 2f + left).toInt()
    }

    private fun isLayoutRtl(): Boolean {
        return layoutDirection == LAYOUT_DIRECTION_RTL
    }

    private fun getScale(): Float {
        val max = mSeekBar.max
        return if (max > 0) mSeekBar.progress / max.toFloat() else 0f
    }

    private fun setup() {
        mTextFontMetricsInt = mTextPaint.fontMetricsInt
        mSeekBarExtraOffsetH = when (mEndpointMode) {
            ENDPOINT_MODE_OFFSET_SEEKBAR -> {
                val textWidth = calculateSimpleTextWidth()
                textWidth / 2
            }
            else -> {
                0
            }
        }
    }

    //计算采样文本宽度
    private fun calculateSimpleTextWidth(): Int {
        val text = mDateFormat.format(Date())
        mTextPaint.getTextBounds(
            text, 0, text.length, mTextRect
        )
        return mTextRect.width()
    }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.FloatTextIndicatorSeekBar,
            defStyleAttr,
            0
        ).use {
            mTextPaint.textSize = it.getDimension(
                R.styleable.FloatTextIndicatorSeekBar_android_textSize,
                context.sp(14)
            )
            mTextPaint.setShadowLayer(
                it.getFloat(R.styleable.FloatTextIndicatorSeekBar_android_shadowRadius, 0f),
                it.getFloat(R.styleable.FloatTextIndicatorSeekBar_android_shadowDx, 0f),
                it.getFloat(R.styleable.FloatTextIndicatorSeekBar_android_shadowDy, 0f),
                it.getColor(
                    R.styleable.FloatTextIndicatorSeekBar_android_shadowColor,
                    Color.TRANSPARENT
                )
            )
            mTextColor = if (it.hasValue(R.styleable.FloatTextIndicatorSeekBar_android_textColor)) {
                it.getColorStateList(R.styleable.FloatTextIndicatorSeekBar_android_textColor)
                    ?: ColorStateList.valueOf(Color.BLACK)
            } else {
                ColorStateList.valueOf(Color.BLACK)
            }
            mTextOffset = it.getDimensionPixelSize(
                R.styleable.FloatTextIndicatorSeekBar_ucsp_floatTextMargin,
                mTextOffset
            )
            val pattern =
                if (it.hasValue(R.styleable.FloatTextIndicatorSeekBar_ucsp_timeShiftPattern))
                    it.getString(R.styleable.FloatTextIndicatorSeekBar_ucsp_timeShiftPattern)
                else DEFAULT_PATTERN
            mDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
            mEndpointMode = it.getInt(
                R.styleable.FloatTextIndicatorSeekBar_ucsp_endpointMode,
                mEndpointMode
            )

            val styleRes =
                it.getResourceId(
                    R.styleable.FloatTextIndicatorSeekBar_ucsp_seekBarStyle,
                    R.style.UcspCtrl_DefaultSeekBarTheme
                )
            mSeekBar = SeekBar(android.view.ContextThemeWrapper(context, styleRes))
//            mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(
//                    seekBar: SeekBar?,
//                    progress: Int,
//                    fromUser: Boolean
//                ) {
////                    if(fromUser){
////                        invalidate()
////                    }
//                }
//
//                override fun onStartTrackingTouch(seekBar: SeekBar?) {
//
//                }
//
//                override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                }
//
//            })
//            mSeekBar.viewTreeObserver.addOnPreDrawListener {
//                invalidate()
//                true
//            }
            addView(mSeekBar, generateDefaultLayoutParams())
        }

        //开启绘制，用于文本展示
        setWillNotDraw(false)
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        if (target == mSeekBar) {
            super.onDescendantInvalidated(child, this)
        } else {
            super.onDescendantInvalidated(child, target)
        }
    }
}