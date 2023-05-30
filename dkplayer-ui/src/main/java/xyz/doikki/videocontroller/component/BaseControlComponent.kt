package xyz.doikki.videocontroller.component

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import unics.player.controller.MediaController
import unics.player.controller.UCSContainerControl
import unics.player.internal.getActivityContext
import unics.player.kernel.UCSPlayerControl
import xyz.doikki.videoplayer.controller.component.ControlComponent

abstract class BaseControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ControlComponent {

    protected var mController: MediaController? = null

    val playerControl: UCSPlayerControl? get() = mController?.playerControl

    val containerControl: UCSContainerControl? get() = mController?.containerControl

    protected val layoutInflater: LayoutInflater get() = LayoutInflater.from(context)

    protected val activity: Activity?
        get() = context.getActivityContext()

    protected fun setViewInFocusMode(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
    }

    override fun attachController(controller: MediaController) {
        mController = controller
    }

    override fun getView(): View? {
        return this
    }


    companion object{

        const val UNDEFINED_LAYOUT = -1
    }

}