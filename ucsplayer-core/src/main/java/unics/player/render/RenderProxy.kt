package unics.player.render

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IntRange
import unics.player.UCSPlayerManager
import unics.player.controller.UCSRenderControl
import unics.player.internal.plogd
import unics.player.internal.plogw
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerBase
import unics.player.orDefault

/**
 * Render代理：只管播Render相关
 * 本类设计：视频渲染层代理，在渲染层基础功能的情况下（旋转、镜像旋转、截屏、比例缩放），新增图层切换、工厂切换、渲染层是否重用等功能控制
 *
 * 注意：注意：注意： 在使用之前，必须调用[bindContainer]指定容器,使用[bindPlayer]绑定播放器，这是两个关键
 * @note 没有通过构造函数注入容器参数主要是考虑通过方法注入会更适合更多情况
 *
 */
class RenderProxy : UCSRender, UCSRenderControl {

    private val mLogPrefix: String = "[RenderProxy@${this.hashCode()}]"

    //Render所在的容器，必须指定
    private lateinit var mContainer: FrameLayout

    private inline val mContext: Context get() = mContainer.context

    //关联的播放器
    private var mAttachedPlayer: UCSPlayerBase? = null

    //真正的渲染视图
    private var mRender: UCSRender? = null

    //自定义Render工厂
    private var mRenderFactory: RenderFactory = UCSPlayerManager.renderFactory

    //render是否可以重用：如果不重用，则每次播放的时候会重新创建一个新的视图层，否则将会使用同一个render
    private var mRenderReusable = UCSPlayerManager.isRenderReusable

    //渲染视图纵横比
    @AspectRatioType
    private var mScreenAspectRatioType = UCSPlayerManager.screenAspectRatioType

    //视频画面大小
    private val mVideoSize = intArrayOf(0, 0)
    private var mSurfaceListener: UCSRender.SurfaceListener? = null
    private var mMirrorRotation: Boolean = false

    @IntRange(from = 0, to = 360)
    private var mVideoRotation: Int = 0

    /**
     * 获取渲染视图的名字
     * @return
     */
    val renderName: String
        get() {
            return if (mRender != null) {
                val className = mRender!!.javaClass.name
                className.substring(className.lastIndexOf(".") + 1)
            } else {
                val className = mRenderFactory.javaClass.name
                className.substring(className.lastIndexOf(".") + 1)
            }
        }

    /**
     * 视频画面大小
     * todo 是否适合直接返回该变量,存在被外层修改的可能？是否应该 return new int[]{mVideoSize[0], mVideoSize[1]}
     */
    override fun getVideoSize(): IntArray {
        return mVideoSize
    }

    /**
     * 绑定Render所在的容器
     * @note 限定了FrameLayout，更适合放render的位置
     */
    fun bindContainer(container: FrameLayout) {
        plogd { "$mLogPrefix bindContainer($container)" }
        mContainer = container
    }

    override fun setRenderReusable(reusable: Boolean) {
        plogd { "$mLogPrefix setRenderReusable($reusable)" }
        mRenderReusable = reusable
    }

    /**
     * 自定义RenderView，继承[RenderFactory]实现自己的RenderView,设置为null则会使用[UCSPlayerManager.renderFactory]
     */
    override fun setRenderViewFactory(factory: RenderFactory?) {
        plogd { "$mLogPrefix setRenderViewFactory($factory)" }
        if (mRenderFactory == factory || (factory == null && mRenderFactory == UCSPlayerManager.renderFactory)) {
            plogd { "$mLogPrefix setRenderViewFactory -> 与当前工厂相同或者与全局工厂相同,即当前工厂并没有发生任何变化，不作任何处理" }
            return
        }
        mRenderFactory = factory.orDefault(UCSPlayerManager.renderFactory)
        plogd { "$mLogPrefix setRenderViewFactory -> $mRenderFactory" }

        //render工厂发生了变化
        //如果之前已存在render，则将以前的render移除释放并重新创建

        plogd { "$mLogPrefix setRenderViewFactory -> releaseCurrentRender" }
        releaseCurrentRender()
        //判断是否需要立马创建render
        mAttachedPlayer?.let {
            plogd { "$mLogPrefix setRenderViewFactory -> attachRender" }
            attachRender(it)
        }
    }

    private fun attachRender(player: UCSPlayerBase) {
        plogd { "$mLogPrefix attachRender" }
        val render = mRender ?: createRender()
        plogd { "$mLogPrefix attachRender render=$render ->  render.bindPlayer($player)" }
        render.bindPlayer(player)
    }

    private fun createRender(): UCSRender {
        return mRenderFactory.create(mContext).also { render ->
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            //设置之前配置
            render.setSurfaceListener(mSurfaceListener)
            render.setAspectRatioType(mScreenAspectRatioType)
            if (mVideoRotation != 0)
                render.setVideoRotation(mVideoRotation)
            render.setMirrorRotation(mMirrorRotation)
            if (mVideoSize[0] > 0 || mVideoSize[1] > 0)
                render.setVideoSize(mVideoSize[0], mVideoSize[1])
            render.view?.let {
                //render添加到最底层
                mContainer.addView(render.view, 0, params)
            }
            mRender = render
            plogd { "$mLogPrefix createRender render=$render" }
        }
    }

    /**
     * 释放当前的render
     */
    private fun releaseCurrentRender() {
        plogd { "$mLogPrefix releaseCurrentRender" }
        mRender?.let {
            releaseRender(it)
        }
        mRender = null
    }

    private fun releaseRender(render: UCSRender) {
        plogd { "$mLogPrefix releaseRender($render)" }
        render.release()
        render.view?.let {
            mContainer.removeView(it)
        }
    }

    /**
     * 重置
     */
    fun reset() {
        plogd { "$mLogPrefix reset" }
        releaseCurrentRender()
        mScreenAspectRatioType = UCSPlayerManager.screenAspectRatioType
        mVideoSize[0] = 0
        mVideoSize[1] = 0
        mMirrorRotation = false
        mVideoRotation = 0
    }

    override val view: View?
        get() = mRender?.view

    override fun bindPlayer(player: UCSPlayerBase?) {
        plogd { "$mLogPrefix bindPlayer： renderReusable=$mRenderReusable currentRenderHashCode=${mRender.hashCode()}" }
        if (!mRenderReusable) {//不重用render，则立即释放当前的render
            plogd { "$mLogPrefix bindPlayer -> releaseCurrentRender renderReusable=$mRenderReusable" }
            releaseCurrentRender()
        }
        mAttachedPlayer = player
        if (player != null) {
            plogd { "$mLogPrefix bindPlayer->  player not null,try attach render to player." }
            attachRender(player)
        } else {
            plogd { "$mLogPrefix bindPlayer-> player is null,ignore." }
        }
    }

    override fun setSurfaceListener(listener: UCSRender.SurfaceListener?) {
        this.mSurfaceListener = listener
        mRender?.setSurfaceListener(listener)
    }

    /**
     * 旋转视频画面
     *
     * @param degree 旋转角度
     */
    override fun setVideoRotation(@IntRange(from = 0, to = 360) degree: Int) {
        mVideoRotation = degree
        mRender?.setVideoRotation(degree)
    }

    override fun setMirrorRotation(enable: Boolean) {
        mMirrorRotation = enable
        mRender?.setMirrorRotation(enable)
    }

    override fun setAspectRatioType(@AspectRatioType aspectRatioType: Int) {
        mScreenAspectRatioType = aspectRatioType
        mRender?.setAspectRatioType(aspectRatioType)
    }

    /**
     * 视频大小发生变化：用于[UCSVideoView]或者持有播放器[UCSPlayer]的对象在[UCSPlayer.EventListener.onVideoSizeChanged]回调时进行调用
     */
    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        mVideoSize[0] = videoWidth
        mVideoSize[1] = videoHeight
        mRender?.setVideoSize(videoWidth, videoHeight)
    }

    override fun screenshot(callback: UCSRender.ScreenShotCallback) {
        super<UCSRender>.screenshot(callback)
    }

    override fun screenshot(highQuality: Boolean, callback: UCSRender.ScreenShotCallback) {
        val render = mRender
        if (render != null) {
            render.screenshot(highQuality, callback)
            return
        }
        plogw { "$mLogPrefix screenshot -> render is null , screenshot is ignored." }
        callback.onScreenShotResult(null)
    }

    /**
     * 释放资源
     */
    override fun release() {
        plogd { "$mLogPrefix release" }
        mAttachedPlayer = null
        releaseCurrentRender()
    }
}