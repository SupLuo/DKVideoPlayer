package droid.unicstar.player.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import unics.player.controller.MediaController
import unics.player.kernel.UCSPlayer
import unics.player.controller.ControlComponent

/**
 * 播放器封面图
 */
@SuppressLint("AppCompatCustomView")
class CoverImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ImageView(context, attrs), ControlComponent {

    protected var mController: MediaController? = null

    override fun attachController(controller: MediaController) {
        this.mController = controller
    }

    override fun getView(): View {
        return this
    }

    override fun onPlayStateChanged(playState: Int) {
        if (playState == UCSPlayer.STATE_ERROR) {
            bringToFront()
            visibility = VISIBLE
        } else if (playState == UCSPlayer.STATE_IDLE) {
            visibility = GONE
        }
    }
}