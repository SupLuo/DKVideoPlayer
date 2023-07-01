package unics.player.control

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import unics.player.controller.ControlComponent
import unics.player.controller.MediaController
import unics.player.kernel.UCSPlayer

/**
 * Created by Lucio on 2021/4/15.
 * 作为封面图
 */
class CoverControlComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), ControlComponent {

//    private var mController: MediaController? = null

    override fun onControllerAttached(controller: MediaController) {
//        this.mController = controller
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

    init {
        if (attrs == null) {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ScaleType.CENTER_CROP
        }
    }
}