package unics.player

import android.app.Activity
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import unics.player.controller.UCSContainerControl
import unics.player.internal.UCSPUtil
import unics.player.internal.plogd
import unics.player.kernel.PlayerProxy
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerControl
import unics.player.widget.ScreenModeHandler
import xyz.doikki.videoplayer.R
import java.lang.ref.SoftReference

/**
 * 本类职责，只是一个壳子，用于访问播放内核和视图层面的功能，不包含任何实际的状态持有
 * 设计的目的是壳子是可以随意替换的，内部的组件保持所有状态和实现所有的功能；
 */
open class UCSVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    //播放器代理
    private val mPlayer: PlayerProxy = PlayerProxy(context),
    //整个容器管理
    private val mDisplayContainer: DisplayContainer = DisplayContainer(context)
) : FrameLayout(context, attrs),
    UCSPlayerControl by mPlayer,
    UCSContainerControl by mDisplayContainer {

    private val TAG = "[UCSVideoView@${this.hashCode()}]"

    //绑定的界面
    private var mBindActivityRef: SoftReference<Activity?>? = null

    private fun requireActivity(): Activity = mBindActivityRef?.get()!!

    private val mActivity: Activity? get() = mBindActivityRef?.get()

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
    protected val playerKernel: UCSPlayer? get() = mPlayer.player

    /**
     * 设置播放地址
     * @param path 播放地址
     */
    fun setDataSource(path: String) {
        plogd { "$TAG setDataSource(path=$path)" }
        mPlayer.setDataSource(context, path)
    }

    /**
     * 设置播放地址
     *
     * @param path    播放地址
     * @param headers 播放地址请求头
     */
    fun setDataSource(path: String, headers: Map<String, String>?) {
        plogd { "$TAG setDataSource(path=$path,headers=$headers)" }
        mPlayer.setDataSource(context, path, headers)
    }

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     */
    fun setDataSource(uri: Uri) {
        plogd { "$TAG setDataSource(uri=$uri)" }
        mPlayer.setDataSource(context, uri)
    }

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     * @param headers 播放地址请求头
     */
    fun setDataSource(uri: Uri, headers: Map<String, String>?) {
        plogd { "$TAG setDataSource(uri=$uri,headers=$headers)" }
        mPlayer.setDataSource(context, uri, headers)
    }

    /**
     * 用于播放raw和asset里面的视频文件
     */
    fun setDataSource(fd: AssetFileDescriptor) {
        plogd { "$TAG setDataSource(fd=$fd)" }
        mPlayer.setDataSource(fd)
    }

    /**
     * 释放（不能在重用）
     * 如果是共享的播放器，在确实需要释放的时候才调用哦
     */
    override fun release() {
        plogd { "$TAG release" }
        //todo 考虑共享播放器释放问题，应该需要从全局去移除
        mPlayer.release()
        //释放render
        mDisplayContainer.release()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && isFullScreen()) {
            mActivity?.let {
                //todo tv/盒子开发不要处理
                //重新获得焦点时保持全屏状态
                ScreenModeHandler.hideSystemBar(it)
            }
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

    fun bindActivity(activity: Activity) {
        mBindActivityRef = SoftReference(activity)
        mDisplayContainer.bindActivity(activity)
    }

    private fun bindDisplayContainer(displayContainer: DisplayContainer) {
        displayContainer.bindContainer(this)
        displayContainer.bindPlayer(this)
    }

    /**
     * 解析对应参数：一般是从外层容器从解析
     */
    private fun applyAttributes(context: Context, attrs: AttributeSet) {
        //读取xml中的配置，并综合全局配置
        val ta = context.obtainStyledAttributes(attrs, R.styleable.UCSVideoView)
        val audioFocus: Boolean =
            ta.getBoolean(
                R.styleable.UCSVideoView_ucsp_enableAudioFocus,
                UCSPManager.isAudioFocusEnabled
            )
        val looping = ta.getBoolean(R.styleable.UCSVideoView_ucsp_looping, false)
        mPlayer.setEnableAudioFocus(audioFocus)
        mPlayer.setLooping(looping)

        if (ta.hasValue(R.styleable.UCSDisplayContainer_ucsp_screenScaleType)) {
            val screenAspectRatioType =
                ta.getInt(
                    R.styleable.UCSDisplayContainer_ucsp_screenScaleType,
                    UCSPManager.screenAspectRatioType
                )
            setAspectRatioType(screenAspectRatioType)
        }
        val playerBackgroundColor =
            ta.getColor(
                R.styleable.UCSDisplayContainer_ucsp_playerBackgroundColor,
                UCSPManager.defaultPlayerBackgroundColor
            )
        setPlayerBackgroundColor(playerBackgroundColor)
        ta.recycle()
    }

    init {
        bindDisplayContainer(mDisplayContainer)
        if (attrs != null) {
            applyAttributes(context, attrs)
        }

        //绑定界面
        val activity = UCSPUtil.getActivityContext(context)
        if (activity != null) {
            bindActivity(activity)
        }
    }
}