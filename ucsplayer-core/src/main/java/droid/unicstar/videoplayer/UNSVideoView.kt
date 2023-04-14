package droid.unicstar.videoplayer

import android.app.Activity
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import droid.unicstar.videoplayer.controller.MediaController
import droid.unicstar.videoplayer.controller.UNSContainerControl
import droid.unicstar.videoplayer.controller.UNSPlayerControl
import droid.unicstar.videoplayer.controller.UNSRenderControl
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.render.AspectRatioType
import droid.unicstar.videoplayer.widget.ScreenModeHandler
import droid.unicstar.videoplayer.controller.UNSVideoViewControl

/**
 * 本类职责，只是一个壳子，用于访问播放内核和视图层面的功能，不包含任何实际的状态持有
 * 设计的目的是壳子是可以随意替换的，内部的组件保持所有状态和实现所有的功能；
 */
open class UNSVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    //播放器
    private val mPlayer: UNSPlayerProxy = UNSPlayerProxy(context),
    private val mDisplayContainer: UNSDisplayContainer = UNSDisplayContainer(context)
) : FrameLayout(context, attrs),
    UNSVideoViewControl,
    UNSPlayerControl by mPlayer,
    UNSContainerControl by mDisplayContainer {

    private val mActivity: Activity get() = mPreferredActivity!!

    //获取Activity，优先通过Controller去获取Activity
    private val mPreferredActivity: Activity? get() = context.getActivityContext()

    /**
     * 获取播放器名字
     */
    val playerName: String get() = mPlayer.playerName

    /**
     * 获取渲染视图的名字
     */
    val renderName: String get() = mDisplayContainer.renderName

    /**
     * 真正的播放器内核
     */
    protected val kernel: UNSPlayer? get() = mPlayer.kernel

//    private val mStateChangeListener = UNSPlayer.OnPlayStateChangeListener { playState ->
//        when (playState) {
//            STATE_PREPARING -> {
//                attachMediaController()
//            }
//        }
//    }

    /**
     * 设置播放地址
     * @param path 播放地址
     */
    fun setDataSource(path: String) {
        mPlayer.setDataSource(context, path)
    }

    /**
     * 设置播放地址
     *
     * @param path    播放地址
     * @param headers 播放地址请求头
     */
    fun setDataSource(path: String, headers: Map<String, String>?) {
        mPlayer.setDataSource(context, path, headers)
    }

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     */
    fun setDataSource(uri: Uri) {
        mPlayer.setDataSource(context, uri)
    }

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     * @param headers 播放地址请求头
     */
    fun setDataSource(uri: Uri, headers: Map<String, String>?) {
        mPlayer.setDataSource(context, uri, headers)
    }

    /**
     * 用于播放raw和asset里面的视频文件
     */
    fun setDataSource(fd: AssetFileDescriptor) {
        mPlayer.setDataSource(fd)
    }

    /**
     * 开始播放，注意：调用此方法后必须调用[.release]释放播放器，否则会导致内存泄漏
     */
    override fun start() {
        attachMediaController()
        mPlayer.start()
    }

    override fun replay(resetPosition: Boolean) {
        mPlayer.replay(resetPosition)
        attachMediaController()
    }

    private fun attachMediaController() {
        mDisplayContainer.bindPlayer(mPlayer)
    }

    /**
     * 释放播放器
     * 如果是共享的播放器，在确实需要释放的时候才调用哦
     */
    override fun release() {
        //todo 考虑共享播放器释放问题，应该需要从全局去移除
        mPlayer.release()
        //释放render
        mDisplayContainer.release()
    }

    /**
     * 设置[.mPlayerContainer]的背景色
     */
    fun setPlayerBackgroundColor(color: Int) {
        mDisplayContainer.setBackgroundColor(color)
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    fun setVideoController(mediaController: MediaController?) {
        mediaController?.setMediaPlayer(this)
        mDisplayContainer.setVideoController(mediaController)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isFullScreen()) {
            //todo tv/盒子开发不要处理
            //重新获得焦点时保持全屏状态
            ScreenModeHandler.hideSystemBar(mActivity)
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    fun onBackPressed(): Boolean {
        return mDisplayContainer.onBackPressed()
    }

//    override fun onSaveInstanceState(): Parcelable? {
//        L.d("onSaveInstanceState: currentPosition=$mSeekWhenPrepared")
//        //activity切到后台后可能被系统回收，故在此处进行进度保存
//        saveCurrentPlayedProgress()
//        return super.onSaveInstanceState()
//    }//读取播放进度


    companion object {
        /**
         * 屏幕比例类型
         */
        const val SCREEN_ASPECT_RATIO_DEFAULT = AspectRatioType.DEFAULT_SCALE
        const val SCREEN_ASPECT_RATIO_SCALE_18_9 = AspectRatioType.SCALE_18_9
        const val SCREEN_ASPECT_RATIO_SCALE_16_9 = AspectRatioType.SCALE_16_9
        const val SCREEN_ASPECT_RATIO_SCALE_4_3 = AspectRatioType.SCALE_4_3
        const val SCREEN_ASPECT_RATIO_MATCH_PARENT = AspectRatioType.MATCH_PARENT
        const val SCREEN_ASPECT_RATIO_SCALE_ORIGINAL = AspectRatioType.SCALE_ORIGINAL
        const val SCREEN_ASPECT_RATIO_CENTER_CROP = AspectRatioType.CENTER_CROP

        /**
         * 普通模式
         */
        const val SCREEN_MODE_NORMAL = UNSContainerControl.SCREEN_MODE_NORMAL

        /**
         * 全屏模式
         */
        const val SCREEN_MODE_FULL = UNSContainerControl.SCREEN_MODE_FULL

        /**
         * 小窗模式
         */
        const val SCREEN_MODE_TINY = UNSContainerControl.SCREEN_MODE_TINY
    }

    init {
        //准备播放器容器
        if (mDisplayContainer.parent != this) {
            mDisplayContainer.removeFromParent()
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            this.addView(mDisplayContainer, params)
            attrs?.let {
                mDisplayContainer.applyAttributes(context, attrs)
            }
        }

        //绑定所属容器
        mDisplayContainer.bindContainer(this)
        //绑定界面
        val activity = context.getActivityContext()
        if (activity != null) {
            mDisplayContainer.bindActivity(activity)
        }
//        mPlayer.addOnPlayStateChangeListener(mStateChangeListener)
    }
}