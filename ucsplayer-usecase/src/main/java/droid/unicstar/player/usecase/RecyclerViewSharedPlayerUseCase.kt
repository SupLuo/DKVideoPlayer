package droid.unicstar.player.usecase

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import unics.player.UCSVideoView
import unics.player.internal.UCSPUtil

import unics.player.controller.ControlComponent


/**
 * 在列表中公用一个播放器
 */
class RecyclerViewSharedPlayerUseCase(
    private val recycler: RecyclerView,
    private val videoView: UCSVideoView
) {
    interface SharedPlayerViewHolder {

        /**
         * 获取播放器的容器
         */
        fun getVideoViewContainer(): ViewGroup

        /**
         * 获取游离的组件
         */
        fun getDissociateControlComponent(): List<ControlComponent>?
    }

    init {

    }

    private fun resetVideoView() {
        val parentGroup = videoView.parent as? ViewGroup
        parentGroup?.removeView(videoView)
////        不用释放播放器
//        videoView.release()
        if (videoView.isFullScreen()) {
            videoView.stopVideoViewFullScreen()
        }
        UCSPUtil.getActivityContext(parentGroup?.context ?: videoView.context)?.let { activity ->
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }


}