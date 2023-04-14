package xyz.doikki.videocontroller.component

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import droid.unicstar.videoplayer.controller.MediaController
import droid.unicstar.videoplayer.controller.UNSVideoViewControl
import xyz.doikki.videoplayer.controller.component.ControlComponent
import xyz.doikki.videoplayer.util.PlayerUtils

abstract class BaseControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ControlComponent {

    protected var mController: MediaController? = null

    protected val player: UNSVideoViewControl? get() = mController?.playerControl

    protected val layoutInflater: LayoutInflater get() = LayoutInflater.from(context)

    protected val activity: Activity?
        get() = PlayerUtils.scanForActivity(context)

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