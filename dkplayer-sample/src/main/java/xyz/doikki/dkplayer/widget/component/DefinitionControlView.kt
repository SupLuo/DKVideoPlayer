package xyz.doikki.dkplayer.widget.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import xyz.doikki.dkplayer.R
import xyz.doikki.videocontroller.component.VodControlView
import xyz.doikki.videoplayer.DKVideoView
import xyz.doikki.videoplayer.util.L
import xyz.doikki.videoplayer.util.PlayerUtils
import kotlin.properties.Delegates

/**
 * 带切换清晰度的控制器
 */
class DefinitionControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    layoutId: Int = R.layout.layout_definition_control_view
) : VodControlView(context, attrs, defStyleAttr, layoutId) {
    private lateinit var mDefinition: TextView
    private lateinit var mPopupWindow: PopupWindow
    private var mRateStr: MutableList<String> = mutableListOf()
    private var mPopLayout: LinearLayout by Delegates.notNull()
    private var mCurIndex = 0

    private var mMultiRateData: MutableMap<String, String> = mutableMapOf()

    /**
     * 码率切换回调
     */
    private var mOnRateSwitchListener: OnRateSwitchListener? = null


    private fun showRateMenu() {
        mPopLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        mPopupWindow.showAsDropDown(
            mDefinition, -((mPopLayout.measuredWidth - mDefinition.measuredWidth) / 2),
            -(mPopLayout.measuredHeight + mDefinition.measuredHeight + PlayerUtils.dp2px(
                context,
                10f
            ))
        )
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        super.onVisibilityChanged(isVisible, anim)
        if (!isVisible) {
            mPopupWindow.dismiss()
        }
    }

    override fun onScreenModeChanged(screenMode: Int) {
        super.onScreenModeChanged(screenMode)
        if (screenMode == DKVideoView.SCREEN_MODE_FULL) {
            mDefinition!!.visibility = VISIBLE
        } else {
            mDefinition!!.visibility = GONE
            mPopupWindow!!.dismiss()
        }
    }


    fun setData(multiRateData: Map<String, String>?) {
        mMultiRateData.clear()
        if(multiRateData.isNullOrEmpty()){

            return
        }
        mMultiRateData.putAll(multiRateData)

        if (mDefinition.text.isNullOrEmpty()) {
            L.d("multiRate")
            if (multiRateData == null) return
            mRateStr = mutableListOf()
            var index = 0
            val iterator: ListIterator<Map.Entry<String, String>> =
                ArrayList<Map.Entry<String, String>>(multiRateData.entries).listIterator(
                    multiRateData.size
                )
            while (iterator.hasPrevious()) { //反向遍历
                val (key) = iterator.previous()
                mRateStr.add(key)
                val rateItem = LayoutInflater.from(context)
                    .inflate(R.layout.layout_rate_item, null) as TextView
                rateItem.text = key
                rateItem.tag = index
                rateItem.setOnClickListener(rateOnClickListener)
                mPopLayout.addView(rateItem)
                index++
            }
            (mPopLayout.getChildAt(index - 1) as TextView).setTextColor(
                ContextCompat.getColor(
                    context, R.color.theme_color
                )
            )
            mDefinition.setText(mRateStr.get(index - 1))
            mCurIndex = index - 1
        }
    }

    private val rateOnClickListener = object :OnClickListener {
        override fun onClick(v: View) {
            val index = v.tag as Int
            if (mCurIndex == index) return
            (mPopLayout.getChildAt(mCurIndex) as TextView).setTextColor(Color.BLACK)
            (mPopLayout.getChildAt(index) as TextView).setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.theme_color
                )
            )
            mDefinition.text = mRateStr[index]
            switchDefinition(mRateStr[index])
            mPopupWindow.dismiss()
            mCurIndex = index
        }
    }

    private fun switchDefinition(s: String) {
        val controller = controller
        if (controller != null) {
            controller.hide()
            controller.stopUpdateProgress()
        }
        val url = mMultiRateData[s]!!
        mOnRateSwitchListener?.onRateChange(url)
    }

    fun setOnRateSwitchListener(onRateSwitchListener: OnRateSwitchListener) {
        mOnRateSwitchListener = onRateSwitchListener
    }

    interface OnRateSwitchListener {
        fun onRateChange(url: String)
    }

    init {
        mPopupWindow =
            PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mPopLayout = LayoutInflater.from(context)
            .inflate(R.layout.layout_rate_pop, this, false) as LinearLayout
        mPopupWindow.contentView = mPopLayout
        mPopupWindow.setBackgroundDrawable(ColorDrawable(-0x1))
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.isClippingEnabled = false
        mDefinition = findViewById(R.id.tv_definition)
        mDefinition?.setOnClickListener(OnClickListener { showRateMenu() })
    }
}