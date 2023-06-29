package unics.player.control.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.graphics.TypefaceCompat
import unics.player.control.internal.dip
import unics.player.control.internal.orDefault
import unics.player.control.internal.sp
import unics.player.control.internal.use
import xyz.doikki.videocontroller.R
import kotlin.math.abs

class TimeShiftFloatTextIndicatorSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), TimeShiftSeekBar {

    companion object {
        const val ENDPOINT_MODE_OFFSET_TEXT = 0
        const val ENDPOINT_MODE_OFFSET_SEEKBAR = 1
        const val ENDPOINT_MODE_CLIP_TEXT = 2
    }

    private val mSeekBar: SeekBar
    private val mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        //设置水平居中展示
        it.textAlign = Paint.Align.CENTER
    }
    private var mTextFontMetricsInt: Paint.FontMetricsInt? = null
    private var mText: String = "00:00:00"
    private val mTextRect: Rect = Rect()
    private var mTextOffset: Int = context.dip(4).toInt()
    private var mTextColor: ColorStateList

    /**
     * 采样文本
     * 主要用于[mEndpointMode] == [ENDPOINT_MODE_OFFSET_SEEKBAR]时，用于计算float文本所占的最大宽度，从而让seekbar两端留出对应的空间
     */
    var samplingText: String = "00:00:00"

    var textSize: Float = 0f
        set(@Px value) {
            if (abs(value - field) < 1e-10) {
                return
            }
            field = value
            mTextPaint.textSize = value
            onTextSizeChanged()
        }

    private var mEndpointMode: Int = ENDPOINT_MODE_OFFSET_TEXT

    //seekbar额外偏移的距离
    private var mSeekBarExtraOffsetH: Int = 0
    private var mIncludeTextBounds: Boolean = true
    private var mEnsureTextDisplay: Boolean = true
    private var mUserSeekBarChangedListener: SeekBar.OnSeekBarChangeListener? = null

    private val mSeekBarChangedListenerGlue = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            if (fromUser) {
                invalidate()
            }
            mUserSeekBarChangedListener?.onProgressChanged(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            mUserSeekBarChangedListener?.onStartTrackingTouch(seekBar)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            mUserSeekBarChangedListener?.onStopTrackingTouch(seekBar)
        }
    }

    fun setTextColor(@ColorInt colorInt: Int) {
        mTextColor = ColorStateList.valueOf(colorInt)
    }

    fun setTextColor(color: ColorStateList) {
        mTextColor = color
    }

    /**
     * 设置文本
     */
    fun setText(text: String) {
        if (mText == text)
            return
        mText = text
        invalidate()
    }

    override fun setProgress(progress: Int) {
        if (mSeekBar.progress == progress)
            return
        mSeekBar.progress = progress
    }

    /**
     * @param progress 进度条进度
     * @param text 指示器文本
     */
    fun setProgressAndText(progress: Int, text: String) {
        setProgress(progress)
        setText(text)
    }

    override fun setSecondaryProgress(secondaryProgress: Int) {
        if (mSeekBar.secondaryProgress == secondaryProgress)
            return
        mSeekBar.secondaryProgress = secondaryProgress
    }

    override fun setOnSeekBarChangeListener(l: SeekBar.OnSeekBarChangeListener?) {
        mUserSeekBarChangedListener = l
    }

    override fun getProgress(): Int {
        return mSeekBar.progress
    }

    override fun getSecondaryProgress(): Int {
        return mSeekBar.secondaryProgress
    }

    override fun getMax(): Int {
        return mSeekBar.max
    }

    override fun setMax(max: Int) {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //包含文字区域或者不需要保障文字显示完全的情况不需要做额外处理
        if (mIncludeTextBounds || !mEnsureTextDisplay)
            return
        (parent as? ViewGroup)?.let { parent ->
            val textBoundsHeight = calculateTextBoundsHeight() * 2 // 2倍，区域判断大一些，好看点
            if (this.top > textBoundsHeight) {
                //修改parent不裁剪child，以便文字能够显示完全
                parent.clipChildren = false
            } else {
                val childTopMargin = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0
                val expectClipPaddingSize = textBoundsHeight - childTopMargin
                if (parent.paddingTop < expectClipPaddingSize) {
                    parent.setPadding(
                        parent.paddingLeft,
                        expectClipPaddingSize,
                        parent.paddingRight,
                        parent.paddingBottom
                    )
                }
                parent.clipToPadding = false
                parent.clipChildren = false
            }

            val childTopMargin = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0
            if (childTopMargin > textBoundsHeight) {
                //修改parent不裁剪child，以便文字能够显示完全
                parent.clipChildren = false
            } else {
                val parentTopPadding = parent.paddingTop
                if (parentTopPadding < textBoundsHeight) {
                    parent.setPadding(
                        parent.paddingLeft,
                        textBoundsHeight,
                        parent.paddingRight,
                        parent.paddingBottom
                    )
                }
                parent.clipToPadding = false
                parent.clipChildren = false
            }
        }
    }

    private fun requireFontMetricsInt(): Paint.FontMetricsInt {
        return mTextFontMetricsInt ?: mTextPaint.fontMetricsInt.also {
            mTextFontMetricsInt = it
        }
    }

    private fun calculateTextBoundsHeight(): Int {
        val fm = requireFontMetricsInt()
        return fm.bottom - fm.top + mTextOffset
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textBoundsHeight = if (mIncludeTextBounds) {
            calculateTextBoundsHeight()
        } else {
            0
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child == mSeekBar) {
                measureChildWithMargins(
                    child,
                    widthMeasureSpec,
                    mSeekBarExtraOffsetH * 2,//左右预留的空间，避免文本显示不全
                    heightMeasureSpec,
                    textBoundsHeight
                )
            }
        }
        val lp = mSeekBar.layoutParams as MarginLayoutParams
        setMeasuredDimension(
            paddingLeft + mSeekBarExtraOffsetH + lp.leftMargin + mSeekBar.measuredWidth + lp.rightMargin + mSeekBarExtraOffsetH + paddingRight,
            paddingTop + textBoundsHeight + lp.topMargin + mSeekBar.measuredHeight + lp.bottomMargin + paddingBottom
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

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawText(canvas)
    }

    private fun drawText(canvas: Canvas) {
        val text = mText
        if (text.isEmpty())
            return
        mTextPaint.color = mTextColor.getColorForState(drawableState, mTextColor.defaultColor)
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

        val fm = requireFontMetricsInt()
        if (mIncludeTextBounds) {
            //绘制文字
            val distance = (fm.descent - fm.ascent) / 2f - fm.bottom
            val baseline: Float =
                (height - paddingTop - paddingBottom - mSeekBar.height - mTextOffset) / 2 + distance
            canvas.drawText(text, x.toFloat(), baseline, mTextPaint)
        } else {
            //绘制文字
            val baseline =
                fm.bottom / 2f - fm.top / 2f - fm.bottom - height / 2f - mTextOffset
            canvas.drawText(text, x.toFloat(), baseline, mTextPaint)
        }

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

    private fun onTextSizeChanged() {
        mTextFontMetricsInt = mTextPaint.fontMetricsInt
        val prevExtra = mSeekBarExtraOffsetH
        mSeekBarExtraOffsetH = when (mEndpointMode) {
            ENDPOINT_MODE_OFFSET_SEEKBAR -> {
                val textWidth = calculateSimpleTextWidth()
                textWidth / 2
            }
            else -> {
                0
            }
        }
        if (prevExtra != mSeekBarExtraOffsetH) {
            requestLayout()
        }
    }

    //计算采样文本宽度
    private fun calculateSimpleTextWidth(): Int {
        val text = samplingText.ifEmpty { mText }
        mTextPaint.getTextBounds(
            text, 0, text.length, mTextRect
        )
        return mTextRect.width()
    }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.TimeShiftFloatTextIndicatorSeekBar,
            defStyleAttr,
            0
        ).use {
            mIncludeTextBounds = it.getBoolean(
                R.styleable.TimeShiftFloatTextIndicatorSeekBar_ucsp_includeTextBounds,
                mIncludeTextBounds
            )
            mTextPaint.setShadowLayer(
                it.getFloat(
                    R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_shadowRadius,
                    0f
                ),
                it.getFloat(R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_shadowDx, 0f),
                it.getFloat(R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_shadowDy, 0f),
                it.getColor(
                    R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_shadowColor,
                    Color.TRANSPARENT
                )
            )
            val textStyle = it.getInt(
                R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_textStyle,
                Typeface.NORMAL
            )
            mTextPaint.typeface = TypefaceCompat.create(context, Typeface.DEFAULT, textStyle)

            textSize = it.getDimension(
                R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_textSize,
                context.sp(14)
            )

            mTextColor =
                if (it.hasValue(R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_textColor)) {
                    it.getColorStateList(R.styleable.TimeShiftFloatTextIndicatorSeekBar_android_textColor)
                        ?: ColorStateList.valueOf(Color.BLACK)
                } else {
                    ColorStateList.valueOf(Color.BLACK)
                }
            mTextOffset = it.getDimensionPixelSize(
                R.styleable.TimeShiftFloatTextIndicatorSeekBar_ucsp_floatTextMargin,
                mTextOffset
            )
            mEndpointMode = it.getInt(
                R.styleable.TimeShiftFloatTextIndicatorSeekBar_ucsp_endpointMode,
                mEndpointMode
            )

            val styleRes =
                it.getResourceId(
                    R.styleable.TimeShiftFloatTextIndicatorSeekBar_ucsp_seekBarStyle,
                    R.style.UcspCtrl_DefaultSeekBarTheme
                )
            mSeekBar = SeekBar(android.view.ContextThemeWrapper(context, styleRes))
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangedListenerGlue)
            addView(mSeekBar, generateDefaultLayoutParams())
        }

        //开启绘制，用于文本展示
        setWillNotDraw(false)
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        if (target == mSeekBar) {
            invalidate()
        } else {
            super.onDescendantInvalidated(child, target)
        }
    }

}