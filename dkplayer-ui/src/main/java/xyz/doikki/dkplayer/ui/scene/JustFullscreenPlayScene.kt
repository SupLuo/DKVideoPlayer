package xyz.doikki.dkplayer.ui.scene

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import xyz.doikki.videocontroller.R
import xyz.doikki.videocontroller.TVVideoController
import xyz.doikki.videocontroller.component.*
import unics.player.UCSVideoView
import unics.player.controller.MediaController
import xyz.doikki.videoplayer.util.CutoutUtil
import xyz.doikki.videoplayer.util.PlayerUtils

/**
 * 全屏播放的场景（不存在切换）
 */
class JustFullscreenPlayScene private constructor(
    val activity: ComponentActivity,
    private val videoView: UCSVideoView,
    autoRequestOrientation: Boolean
) : BasePlayScene() {

    private lateinit var controller: MediaController
    private lateinit var titleView: TitleView

    init {
        if (autoRequestOrientation) {
            CutoutUtil.adaptCutout(activity, true)
            val activityOrientation = activity.requestedOrientation
            //设置view全屏
            if (activityOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || activityOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                videoView.startVideoViewFullScreen()
            } else {
                videoView.startFullScreen()
            }
//        videoView.requestFocus()
        }
    }

    /**
     * 使用默认的控制器
     */
    fun setControllerDefault(title: CharSequence) {
        setController(createDefaultController(title))
    }

    /**
     * 设置自定义控制器
     */
    fun setController(controller: MediaController) {
        this.controller = controller
        videoView.setVideoController(controller)
    }

    fun setTitle(title: CharSequence?) {
        if (!::controller.isInitialized)
            throw IllegalStateException("请先调用setController方法设置控制器，或者调用setControllerDefault方法使用默认提供的控制器")

        if (::titleView.isInitialized) {
            titleView.setTitle(title)
            return
        }
        controller.findViewById<TextView?>(R.id.title)?.text = title
    }

    fun setOnBackClick(click: View.OnClickListener?) {
        if (!::controller.isInitialized)
            throw IllegalStateException("请先调用setController方法设置控制器，或者调用setControllerDefault方法使用默认提供的控制器")
        if (::titleView.isInitialized) {
            titleView.setOnBackClickListener(click)
            return
        }
        controller.findViewById<View?>(R.id.back)?.setOnClickListener(click)
    }

    override fun getVideoView(): UCSVideoView {
        return videoView
    }

    fun onBackPressed(): Boolean {
//        if (!videoView.onBackPressed()) {
//            activity.finish()
//        }
        activity.finish()
        return true
    }

    /**
     * 默认的控制器
     */
    private fun createDefaultController(title: CharSequence): MediaController {
        return TVVideoController(activity).apply {
            addControlComponent(CompleteView(context))
            addControlComponent(ErrorView(context))
            addControlComponent(PrepareView(context))
            addControlComponent(TitleView(context).also {
                this@JustFullscreenPlayScene.titleView = it
                it.setOnBackClickListener {
                    this@JustFullscreenPlayScene.onBackPressed()
                }
                it.setTitle(title)
            })

            val vodControlView = VodControlView(activity)
            // 我这里隐藏了全屏按钮并且调整了边距，我不推荐这样做，我这样只是为了方便，
            // 如果你想对某个组件进行定制，直接将该组件的代码复制一份，改成你想要的样子
            vodControlView.findViewById<View>(R.id.fullscreen).visibility = View.GONE
            val lp =
                vodControlView.findViewById<View>(R.id.total_time).layoutParams as LinearLayout.LayoutParams
            lp.rightMargin = PlayerUtils.dp2px(context, 16f)
            addControlComponent(vodControlView)
            //最后添加手势view
            addControlComponent(GestureView(context))
        }
    }

    companion object {

        /**
         * 本方法会创建一个[UCSVideoView]播放器并调用[Activity.setContentView]进行设置
         * 必须在[Activity.onCreate]方法或之后调用
         */
        @JvmOverloads
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            autoRequestOrientation: Boolean = true
        ): JustFullscreenPlayScene {
            val currentState = activity.lifecycle.currentState
            println("LifecycleObserver:isAtLeast2-2 $currentState")
            val videoView = UCSVideoView(activity).also {
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            activity.setContentView(videoView)
            return create(activity, videoView, autoRequestOrientation)
        }


        @JvmStatic
        fun create(
            activity: ComponentActivity,
            videoView: UCSVideoView,
            autoRequestOrientation: Boolean = true
        ): JustFullscreenPlayScene {
            return JustFullscreenPlayScene(activity, videoView, autoRequestOrientation)
        }
    }

}