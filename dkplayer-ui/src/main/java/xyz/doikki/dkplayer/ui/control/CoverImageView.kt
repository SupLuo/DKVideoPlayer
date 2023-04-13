package xyz.doikki.dkplayer.ui.control

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import droid.unicstar.videoplayer.UNSVideoView
import droid.unicstar.videoplayer.controller.MediaController
import xyz.doikki.videoplayer.controller.VideoViewControl
import xyz.doikki.videoplayer.controller.component.ControlComponent

/**
 * 播放器封面图
 */
class CoverImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ImageView(context, attrs), ControlComponent {

    protected var mController: MediaController? = null

    protected val player: VideoViewControl? get() = mController?.playerControl

    override fun attachController(controller: MediaController) {
        this.mController = controller
    }

    override fun getView(): View {
        return this
    }

    override fun onPlayStateChanged(playState: Int) {
        if (playState == UNSPlayer.STATE_ERROR) {
            bringToFront()
            visibility = VISIBLE
        } else if (playState == UNSPlayer.STATE_IDLE) {
            visibility = GONE
        }
    }
}