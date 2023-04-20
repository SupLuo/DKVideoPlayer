package droid.unicstar.player.usecase

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import droid.unicstar.player.UCSPlayerManager.add
import droid.unicstar.player.UCSVideoView
import droid.unicstar.player.getActivityContext
import xyz.doikki.videoplayer.controller.component.ControlComponent


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
        (parentGroup?.context ?: videoView.context).getActivityContext()?.let { activity ->
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    /**
     * @param container 添加播放器的容器，即在该容器中播放
     */
    fun play(holder: SharedPlayerViewHolder, indexToAdd: Int = 0) {
        //第一步：如果播放器已经在其他容器中添加，则先移除，并同时考虑临时组件的移除（如果存在）
        //第二步：添加到指定容器，并添加组件（如果需要）
        //第三步：设置数据源并播放

        val container = holder.getVideoViewContainer()
        val parentGroup = videoView.parent as? ViewGroup
        if (parentGroup == container) {
            //当前所属容器与指定容器相同，所以没有发生变化，不用做额外处理
            return
        }
        parentGroup?.let {
            val prvHolder = it.getTag(R.id.ucs_id_shared_holder_tag)
            resetVideoView()
        }

//        container.addView(videoView, indexToAdd)
//        //把列表中预置的PrepareView添加到控制器中，注意isDissociate此处只能为true, 请点进去看isDissociate的解释
//        //把列表中预置的PrepareView添加到控制器中，注意isDissociate此处只能为true, 请点进去看isDissociate的解释
//        mController.addControlComponent(viewHolder.mPrepareView, true)
//        xyz.doikki.dkplayer.util.Utils.removeViewFormParent(mVideoView)
//        viewHolder.mPlayerContainer.addView(mVideoView, 0)
//        //播放之前将VideoView添加到VideoViewManager以便在别的页面也能操作它
//        //播放之前将VideoView添加到VideoViewManager以便在别的页面也能操作它
//        getVideoViewManager().add(mVideoView, xyz.doikki.dkplayer.util.Tag.LIST)
//
//        mVideoView.setDataSource(videoBean.getUrl())
//        mTitleView.setTitle(videoBean.getTitle())
//        mVideoView.start()
//        mCurPos = position
    }
}