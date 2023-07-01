package unics.player.control

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import unics.player.UCSPManager
import unics.player.controller.ControlComponent
import unics.player.controller.MediaController
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

/**
 * update by luochao at 2023/6/30
 * 使用流量播放温馨提示
 * 手机和平板用得上
 */
class NetworkWarningControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @LayoutRes layoutId: Int = BaseControlComponent.UNDEFINED_LAYOUT
) : LinearLayout(context, attrs), ControlComponent {

    private var mController: MediaController? = null

    override fun getView(): View {
        return this
    }

    override fun onControllerAttached(controller: MediaController) {
        this.mController = controller
    }

    override fun onPlayStateChanged(playState: Int) {
        if (playState == UCSPlayer.STATE_PREPARED_BUT_ABORT) {
            visibility = View.VISIBLE
            bringToFront()
        } else {
            visibility = View.GONE
        }
    }

    init {
        if (attrs == null) {
            setBackgroundResource(R.color.ucsp_ctrl_control_component_background_color)
            //默认不显示
            visibility = GONE
            //防止touch模式下，事件穿透
            isClickable = true

            orientation = VERTICAL
            gravity = Gravity.CENTER
        }

        View.inflate(
            context,
            if (layoutId > 0) layoutId else R.layout.ucsp_ctrl_network_warning_control_component,
            this
        )

        findViewById<View?>(R.id.ucsp_ctrl_back)?.setOnClickListener {
            val control = mController?.playerControl ?: return@setOnClickListener
            visibility = View.GONE
            UCSPManager.isPlayOnMobileNetwork = true
            control.start()
        }
    }

}