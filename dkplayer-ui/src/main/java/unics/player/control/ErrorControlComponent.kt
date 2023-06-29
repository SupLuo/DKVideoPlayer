package unics.player.control

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.annotation.LayoutRes
import droid.unicstar.player.ui.TVCompatible
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R
import kotlin.math.abs

/**
 * 播放出错提示界面
 * Created by Doikki on 2017/4/13.
 * update by luochao on022/9/28 调整基类接口变更引起的变动，去掉无用代码
 */
@TVCompatible(message = "不用做什么特殊处理")
class ErrorControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(
    context, attrs, defStyleAttr
) {
    private var mDownX = 0f
    private var mDownY = 0f

    private val mButtonView: View?

    private val mTextView: TextView?

    fun setErrorMessage(message: CharSequence?) {
        mTextView?.text = message
    }

    override fun onPlayStateChanged(playState: Int) {
        visibility = if (playState == UCSPlayer.STATE_ERROR) {
            bringToFront()
            VISIBLE
        } else {
            GONE
        }
    }

    /**
     * 以下逻辑用于小窗展示的情况下，避免在触摸的小范围内滑动窗口
     *
     * @param ev
     * @return
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = ev.x
                mDownY = ev.y
                // True if the child does not want the parent to intercept touch events.
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val absDeltaX = abs(ev.x - mDownX)
                val absDeltaY = abs(ev.y - mDownY)
                if (absDeltaX > ViewConfiguration.get(context).scaledTouchSlop ||
                    absDeltaY > ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    init {
        visibility = GONE
        if (isInEditMode) {
            visibility = VISIBLE
        }
        setBackgroundResource(R.color.ucsp_ctrl_control_component_background_color)
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(R.layout.ucsp_ctrl_error_control_component, this)
        }
        mButtonView = findViewById(R.id.ucsp_ctrl_btn)
        mButtonView?.setOnClickListener {
            visibility = GONE
            mController?.replay(false)
        }

        mTextView = findViewById(R.id.ucsp_ctrl_text)
//        if (isTelevisionUiMode()) {
//            statusBtn.isFocusable = true
//            statusBtn.isFocusableInTouchMode = true
//        } else {
//            //设置当前容器能点击的原因是为了避免事件穿透
//            isClickable = true
//        }
        //设置当前容器能点击的原因是为了避免事件穿透
        isClickable = !isTelevisionUiMode
    }
}