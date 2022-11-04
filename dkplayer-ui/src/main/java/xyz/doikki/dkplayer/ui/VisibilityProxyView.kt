package xyz.doikki.dkplayer.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation

/**
 * 当不允许View响应可见性操作时，则调用[setVisibility]方法会被忽略
 * @param enableVisibility 是否响应[setVisibility]方法
 * @param visibilityWhenDisabled 当[enableVisibility]设置为false时，[view]设置的的可见性
 */
 class VisibilityProxyView(
    private val view: View,
    private var enableVisibility: Boolean = true,
    private var visibilityWhenDisabled: Int = View.GONE
) {

    fun setEnableVisibility(enable: Boolean) {
        enableVisibility = enable
        if (!enable) {
            view.visibility = visibilityWhenDisabled
        }
    }

    /**
     * 设置可见性
     */
    var visibility: Int
        get() = view.visibility
        set(value) {
            if (!enableVisibility) {
                return
            }

            view.visibility = value
        }

    val layoutParams: ViewGroup.LayoutParams? get() = view.layoutParams

    fun setOnClickListener(l: View.OnClickListener?) {
        view.setOnClickListener(l)
    }

    var isSelected: Boolean
        get() = view.isSelected
        set(value) {
            view.isSelected = value
        }

    fun startAnimation(anim: Animation) {
        view.clearAnimation()
        view.startAnimation(anim)
    }
}