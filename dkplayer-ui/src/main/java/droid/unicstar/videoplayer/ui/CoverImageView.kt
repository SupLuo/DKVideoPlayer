package droid.unicstar.videoplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import droid.unicstar.videoplayer.controller.MediaController
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.controller.UNSVideoViewControl
import xyz.doikki.videoplayer.controller.component.ControlComponent

/**
 * 播放器封面图
 */
class CoverImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ImageView(context, attrs), ControlComponent {

    protected var mController: MediaController? = null

    protected val player: UNSVideoViewControl? get() = mController?.playerControl

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