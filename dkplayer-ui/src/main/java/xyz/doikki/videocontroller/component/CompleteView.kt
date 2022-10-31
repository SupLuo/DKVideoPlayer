package xyz.doikki.videocontroller.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import xyz.doikki.videoplayer.DKVideoView
import xyz.doikki.videoplayer.TVCompatible
import xyz.doikki.videoplayer.util.isVisible
import xyz.doikki.videoplayer.util.orDefault

/**
 * 自动播放完成界面
 *
 * 已适配TV
 * update by luochao at 2022/9/28
 *
 * @param layoutId 自定义布局id 默认未指定（未指定的情况下会根据是否是tv模式选择不同的默认布局）
 */
@TVCompatible("兼容TV展示：默认情况不会在TV上显示返回按钮")
class CompleteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @LayoutRes layoutId: Int = UNDEFINED
) : BaseControlComponent(context, attrs) {

    /**
     * 退出全屏按钮
     */
    private val mStopFullscreen: View?

    /**
     * 设置播放结束按钮的文本（默认是“重新播放”）
     *
     * @param message
     */
    fun setCompleteText(message: CharSequence?) {
        findViewById<TextView?>(R.id.tv_replay)?.text = message
    }

    override fun onPlayStateChanged(playState: Int) {
        if (playState == DKVideoView.STATE_PLAYBACK_COMPLETED) {//只有播放结束的时候才显示
            visibility = VISIBLE
            mStopFullscreen?.isVisible = controller?.isFullScreen.orDefault()
            bringToFront()
        } else {
            visibility = GONE
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onScreenModeChanged(screenMode: Int) {
        mStopFullscreen ?: return
        if (screenMode == DKVideoView.SCREEN_MODE_FULL) {
            mStopFullscreen.visibility = VISIBLE
        } else if (screenMode == DKVideoView.SCREEN_MODE_NORMAL) {
            mStopFullscreen.visibility = GONE
        }

        controller?.let { controller ->
            val activity = activity
            if (activity != null && controller.hasCutout == true) {
                val orientation = activity.requestedOrientation
                val cutoutHeight = controller.cutoutHeight
                val sflp = mStopFullscreen.layoutParams as LayoutParams
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        sflp.setMargins(0, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        sflp.setMargins(cutoutHeight, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        sflp.setMargins(0, 0, 0, 0)
                    }
                }
            }
        }
    }

    init {
        visibility = GONE
        if (layoutId <= 0) {
            layoutInflater.inflate(
                if (isTelevisionUiMode()) R.layout.dkplayer_layout_complete_view_tv else R.layout.dkplayer_layout_complete_view,
                this
            )
        } else {
            layoutInflater.inflate(layoutId, this)
        }

        //在xml中去除了一个布局层级，因此xml中的背景色改为代码设置在当前布局中
        setBackgroundColor(Color.parseColor("#33000000"))
        //防止touch模式下，事件穿透
        isClickable = true

        findViewById<View?>(R.id.replay_layout)?.let {
//            if (isTelevisionUiMode()) {
//                it.isClickable = true
//                setViewInFocusMode(it)
//            } else {
//                //防止touch模式下，事件穿透
//                isClickable = true
//            }
            it.setOnClickListener { //重新播放
                controller?.replay(true)
            }
        }

        mStopFullscreen = findViewById(R.id.stop_fullscreen)
        mStopFullscreen?.setOnClickListener {
            controller?.let { controller ->
                if (controller.isFullScreen) {
                    val activity = activity
                    if (activity != null && !activity.isFinishing) {
                        controller.stopFullScreen()
                    }
                }
            }
        }
    }

}