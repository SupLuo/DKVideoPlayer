package unics.player.control.leanback

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.LayoutRes
import droid.unicstar.player.ui.TVCompatible
import unics.player.control.VodControlComponent
import xyz.doikki.videocontroller.R

/**
 * 点播底部控制栏
 */
@TVCompatible(message = "TV上不显示全屏按钮")
open class LeanbackVodControlComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = R.layout.ucsp_ctrl_vod_control_layout_leanback
) : VodControlComponent(context, attrs, defStyleAttr, layoutId)