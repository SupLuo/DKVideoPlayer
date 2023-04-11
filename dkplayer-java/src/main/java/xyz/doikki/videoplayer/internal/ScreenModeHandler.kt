package xyz.doikki.videoplayer.internal

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.Px
import xyz.doikki.videoplayer.R
import xyz.doikki.videoplayer.util.contentView
import xyz.doikki.videoplayer.util.decorView
import xyz.doikki.videoplayer.util.removeFromParent

/**
 * 处理播放器屏幕切换
 *
 * @see .startFullScreen
 * @see .stopFullScreen
 * @see .startTinyScreen
 * @see .stopTinyScreen
 * @see .setTinyScreenSize 设置小窗大小
 */
class ScreenModeHandler {

    /**
     * / **
     * 小屏窗口大小
     */
    private val mTinyScreenSize = intArrayOf(0, 0)

    /**
     * 推荐的小窗宽度
     */
    @Px
    private var mPreferredTinyScreenWidth = 0

    /**
     * 推荐的小窗高度
     */
    @Px
    private var mPreferredTinyScreenHeight = 0

    /**
     * 设置小屏的宽高
     */
    fun setTinyScreenSize(width: Int, height: Int) {
        mTinyScreenSize[0] = width
        mTinyScreenSize[1] = height
    }

    /**
     * 切换到全屏
     *
     * @param view 用于全屏展示的view
     * @param hideSystemBar 隐藏状态栏：true 则表示同时隐藏状态栏，false则不做处理
     * @note hideSystemBar：在某些盒子上，全屏的时候不需要处理状态栏相关的操作，否则会在某些盒子上显示系统提示导致界面无法操作
     */
    fun startFullScreen(activity: Activity, view: View, hideSystemBar: Boolean = true): Boolean {
        val decorView = activity.decorView ?: return false

        if (hideSystemBar) {
            //隐藏NavigationBar和StatusBar
            hideSystemBar(activity, decorView)
        }

        //从parent中移除指定view
        view.removeFromParent()
        //将视图添加到DecorView中即实现了全屏展示该控件
        decorView.addView(view)
        return true
    }

    /**
     * 退出全屏
     *
     * @param container view退出全屏之后的容器
     * @param view      全屏展示的view ： 本身这个参数可以不传，但是还是保留，这样更明确逻辑
     * @param showSystemBar 显示状态栏：true 则表示同时隐藏状态栏，false则不做处理
     * @note showSystemBar：在某些盒子上，全屏的时候不需要处理状态栏相关的操作，否则会在某些盒子上显示系统提示导致界面无法操作
     */
    fun stopFullScreen(activity: Activity, container: ViewGroup, view: View, showSystemBar: Boolean = true): Boolean {
        activity.decorView?.let {
            if(showSystemBar)
            showSystemBar(activity, it)//显示状态栏
        }
        view.removeFromParent()
        container.addView(view)
        return true
    }

    /**
     * 开启小屏
     *
     * @param view 在小窗口中显示的View
     */
    fun startTinyScreen(activity: Activity, view: View): Boolean {
        val contentView = activity.contentView ?: return false
        view.removeFromParent()
        //缓存原来的布局参数
        val layoutParamsCache = view.layoutParams
        view.setTag(R.id.screen_mode_layout_params, layoutParamsCache)
        val width = getTinyScreenWidth(activity)
        val height = getTinyScreenHeight(activity)
        val params = FrameLayout.LayoutParams(width, height)
        params.gravity = Gravity.BOTTOM or Gravity.END
        contentView.addView(view, params)
        return true
    }

    /**
     * 退出小屏
     *
     * @param container 用于添加view
     * @param view      在小窗口中显示的view：本身这个参数可以不传，但是还是保留，这样更明确逻辑
     */
    fun stopTinyScreen(container: ViewGroup, view: View): Boolean {
        view.removeFromParent()
        var lp = view.getTag(R.id.screen_mode_layout_params) as? ViewGroup.LayoutParams
        if (lp == null) {
            lp = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(view, lp)
        return true
    }

    /**
     * 获取小窗宽度
     */
    @Px
    private fun getTinyScreenWidth(activity: Activity): Int {
        val width = mTinyScreenSize[0]
        if (width > 0) return width
        setupPreferredTinyScreenSize(activity)
        return mPreferredTinyScreenWidth
    }

    /**
     * 获取小窗高度
     */
    @Px
    private fun getTinyScreenHeight(activity: Activity): Int {
        val height = mTinyScreenSize[1]
        return if (height > 0) height else mPreferredTinyScreenHeight
    }

    /**
     * 初始化默认的小窗大小
     */
    private fun setupPreferredTinyScreenSize(activity: Activity) {
        if (mPreferredTinyScreenWidth > 0) return
        mPreferredTinyScreenWidth = activity.resources.displayMetrics.widthPixels / 2
        mPreferredTinyScreenHeight = mPreferredTinyScreenWidth * 9 / 16
    }


    companion object {

        /**
         * 显示系统状态栏(NavigationBar和StatusBar)
         */
        @JvmStatic
        fun showSystemBar(activity: Activity) {
            val decorView = activity.decorView ?: return
            showSystemBar(activity, decorView)
        }

        /**
         * 显示系统状态栏(NavigationBar和StatusBar)
         */
        @JvmStatic
        fun showSystemBar(activity: Activity, decorView: ViewGroup) {
            var uiOptions = decorView.systemUiVisibility
            uiOptions = uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions = uiOptions and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
            }
            decorView.systemUiVisibility = uiOptions
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        /**
         * 隐藏系统状态栏(NavigationBar和StatusBar)
         *
         * @param activity
         */
        @JvmStatic
        fun hideSystemBar(activity: Activity) {
            val decorView = activity.decorView ?: return
            hideSystemBar(activity, decorView)
        }

        /**
         * 隐藏系统状态栏(NavigationBar和StatusBar)
         *
         * @param activity
         */
        @JvmStatic
        fun hideSystemBar(activity: Activity, decorView: ViewGroup) {
            var uiOptions = decorView.systemUiVisibility
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            decorView.systemUiVisibility = uiOptions
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}