package xyz.doikki.videocontroller.scene

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import xyz.doikki.videocontroller.R
import xyz.doikki.videocontroller.StandardVideoController
import xyz.doikki.videocontroller.component.*
import xyz.doikki.videoplayer.DKVideoView
import xyz.doikki.videoplayer.util.CutoutUtil
import xyz.doikki.videoplayer.util.PlayerUtils

/**
 * 只用于全屏播放的场景
 */

open class JustFullscreenPlayScene private constructor(
    private val activity: Activity,
    val dkVideoView: DKVideoView
) {

    val controller: StandardVideoController

    init {
        CutoutUtil.adaptCutoutAboveAndroidP(activity, true)
        //设置video view 横屏展示
        if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            dkVideoView.startFullScreen()
        } else {
            dkVideoView.startVideoViewFullScreen()
        }

        controller = StandardVideoController(activity)
        setupVideoView()
    }

    protected fun setupVideoView() {
        controller.addControlComponent(CompleteView(activity))
        controller.addControlComponent(ErrorView(activity))
        controller.addControlComponent(PrepareView(activity))

        val vodControlView = VodControlView(activity)
        // 我这里隐藏了全屏按钮并且调整了边距，我不推荐这样做，我这样只是为了方便，
        // 如果你想对某个组件进行定制，直接将该组件的代码复制一份，改成你想要的样子
        vodControlView.findViewById<View?>(R.id.ctrl_fullscreen).visibility = View.GONE
        val lp =
            vodControlView.findViewById<View>(R.id.total_time).layoutParams as LinearLayout.LayoutParams
        lp.rightMargin = PlayerUtils.dp2px(activity, 16f)
        controller.addControlComponent(vodControlView)
        controller.addControlComponent(GestureView(activity))
        dkVideoView.videoController = controller
        dkVideoView.setScreenAspectRatioType(DKVideoView.SCREEN_ASPECT_RATIO_SCALE_16_9)
    }

    companion object {

        /**
         * 整个界面布局都不用，直接就一播放器的场景，该方法会创建一个播放器设置为界面的content view
         */
        @JvmStatic
        fun bind(activity: Activity): JustFullscreenPlayScene {
            val videoView = DKVideoView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            activity.setContentView(videoView)
            return bind(activity, videoView)
        }

        @JvmStatic
        fun bind(activity: Activity, videoView: DKVideoView): JustFullscreenPlayScene {
            return JustFullscreenPlayScene(activity, videoView)
        }

    }

}