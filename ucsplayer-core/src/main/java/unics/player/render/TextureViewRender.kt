package unics.player.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import unics.player.UCSPlayerManager
import unics.player.internal.plogd2
import unics.player.internal.plogi2
import unics.player.internal.plogv2
import unics.player.kernel.UCSPlayer
import unics.player.kernel.UCSPlayerBase
import unics.player.render.UCSRender.Companion.createShotBitmap
import unics.player.render.UCSRender.ScreenShotCallback
import unics.player.render.UCSRender.SurfaceListener
import java.lang.ref.WeakReference

/**
 * 经查看[TextureView]源码，发现在[.onDetachedFromWindow]的时候，
 * 会先回调[TextureView.SurfaceTextureListener.onSurfaceTextureDestroyed]并释放[SurfaceTexture],
 * 在需要[SurfaceTexture]时会重新构建并回调[TextureView.SurfaceTextureListener.onSurfaceTextureAvailable]
 *
 * @see UCSRender 具体可调用的方法请查看Render
 */
class TextureViewRender @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextureView(context, attrs), UCSRender {

    private val mLogPrefix = "[TextureViewRender@${this.hashCode()}]"
    private var mReleased: Boolean = false
    private val mHelper: RenderHelper = RenderHelper.create(this)
    private var mPlayerRef: WeakReference<UCSPlayerBase>? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurface: Surface? = null
    private var mSurfaceListener: SurfaceListener? = null
    private val mEnableRenderOptimization: Boolean =
        UCSPlayerManager.isTextureViewRenderOptimizationEnabled
    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            plogi2(mLogPrefix) { "onSurfaceTextureAvailable $surface $width $height ${mPlayerRef?.get()}" }
            if (mEnableRenderOptimization) {
                plogd2(mLogPrefix) { "onSurfaceTextureAvailable ->render optimization." }
                //开启渲染优化
                if (mSurfaceTexture == null) {
                    mSurfaceTexture = surface
                    mSurface = Surface(surface).also {
                        tryBindSurfaceToPlayer(it)
                    }
                } else {
                    plogd2(mLogPrefix) { "onSurfaceTextureAvailable ->use previous surface texture." }
                    //在开启优化的情况下，使用最开始的那个渲染器
                    setSurfaceTexture(mSurfaceTexture!!)
                }
            } else {
                mSurface = Surface(surfaceTexture).also {
                    tryBindSurfaceToPlayer(it)
                }
            }
            notifySurfaceAvailable(mSurface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            plogi2(mLogPrefix) { "onSurfaceTextureSizeChanged $surface $width $height" }
            mSurfaceListener?.onSurfaceSizeChanged(mSurface, width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            plogi2(mLogPrefix) { "onSurfaceTextureDestroyed $surface" }
            //清空释放
            mSurfaceListener?.onSurfaceDestroyed(mSurface)
            return if (mEnableRenderOptimization) {
                plogd2(mLogPrefix) { "onSurfaceTextureDestroyed -> enable render optimization,return ${mSurfaceTexture == null}" }
                //如果开启了渲染优化，那mSurfaceTexture通常情况不可能为null（在onSurfaceTextureAvailable初次回调的时候被赋值了），
                // 所以这里通常返回的是false，返回值false会告诉父类不要释放SurfaceTexture
                mSurfaceTexture == null
            } else {
                plogd2(mLogPrefix) { "onSurfaceTextureDestroyed -> disable render optimization,return true." }
                true
            }
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            plogv2(mLogPrefix) { "onSurfaceTextureUpdated $surface" }
            if (mEnableRenderOptimization && mSurfaceTexture == null) {
                plogi2(mLogPrefix) { "onSurfaceTextureUpdated mEnableRenderOptimization && mSurfaceTexture == null -> 用于修正开启渲染优化的情况下，mSurfaceTexture为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法" }
                //用于修正开启渲染优化的情况下，mSurfaceTexture为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法
                mSurfaceTexture = surface
                mSurface = Surface(surface)
                tryBindSurfaceToPlayer(mSurface!!)
                notifySurfaceAvailable(mSurface, width, height)
            } else if (mSurface == null) {
                plogi2(mLogPrefix) { "onSurfaceTextureUpdated mSurface == null -> 用于修正未开启渲染优化的情况下，mSurface为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法" }
                //用于修正未开启渲染优化的情况下，mSurface为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法
                mSurface = Surface(surfaceTexture)
                tryBindSurfaceToPlayer(mSurface!!)
                notifySurfaceAvailable(mSurface, width, height)
            } else {
                //onSurfaceTextureUpdated会在SurfaceTexture.updateTexImage()的时候回调该方法，因此只要图像更新，都会调用本方法，因此在这个方法中不适合做什么处理
//                Log.d("TextureView", "onSurfaceTextureUpdated $surface")
//                mSurfaceListener?.onSurfaceUpdated(mSurface)
            }
        }

        private fun tryBindSurfaceToPlayer(surface: Surface) {
            mPlayerRef?.get()?.setSurface(surface)
        }

        private fun notifySurfaceAvailable(surface: Surface?, width: Int, height: Int) {
            mSurfaceListener?.onSurfaceAvailable(surface)
        }
    }

    /**
     * 绑定播放器
     *
     * @param player
     */
    override fun bindPlayer(player: UCSPlayerBase?) {
        plogi2(mLogPrefix) { "bindPlayer" }
        checkAvailable()
        val prvPlayer = mPlayerRef?.get()
        if (player == null) {
            plogi2(mLogPrefix) { "bindPlayer -> player is null,unbind previous player." }
            //相当于解除绑定
            val sSurface = mSurface
            //如果之前的播放器不为空，并且当前holder可用，则先解除holder引用，再解除player的引用
            if (prvPlayer != null && sSurface != null) {
                plogi2(mLogPrefix) { "bindPlayer -> previous player is not null and surface is available,set prv player display surface null." }
                prvPlayer.setSurface(null)
            }
            mPlayerRef = null
            return
        }
        mPlayerRef = WeakReference(player)
        //当前surface不为空，则说明是surface重用
        mSurface?.let {
            if (prvPlayer != player) {
                plogi2(mLogPrefix) { "bindPlayer-> player param and previous player ref are not null ,and they are not the same,set prv player display holder null." }
                prvPlayer?.setSurface(null)
            }
            plogi2(mLogPrefix) { "bindPlayer-> surface holder is available ,attach to player directly." }
            player.setSurface(it)
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        plogi2(mLogPrefix) { "setVideoSize(videoWidth=$videoWidth,videoHeight=$videoHeight)" }
        checkAvailable()
        mHelper.setVideoSize(videoWidth, videoHeight)
    }

    /*************START Render 实现逻辑 */

    override val view: View = this

    override fun setSurfaceListener(listener: SurfaceListener?) {
        checkAvailable()
        mSurfaceListener = listener
    }

    override fun screenshot(highQuality: Boolean, callback: ScreenShotCallback) {
        checkAvailable()
        if (!isAvailable) {
            callback.onScreenShotResult(null)
            return
        }
        if (highQuality) {
            callback.onScreenShotResult(bitmap)
        } else {
            callback.onScreenShotResult(getBitmap(createShotBitmap(this, false)))
        }
    }

    override fun setAspectRatioType(aspectRatioType: Int) {
        checkAvailable()
        mHelper.setAspectRatioType(aspectRatioType)
    }

    override fun setVideoRotation(degree: Int) {
        checkAvailable()
        mHelper.setVideoRotation(degree)
    }

    override fun setMirrorRotation(enable: Boolean) {
        checkAvailable()
        mHelper.setMirrorRotation(enable)
    }

    override fun release() {
        //释放之后便不可再使用
        if (mReleased)
            return
        mReleased = true
        surfaceTextureListener = null
        mPlayerRef = null
        mSurface?.release()
        mSurfaceTexture?.release()
        mSurface = null
        mSurfaceTexture = null
    }

    /*************END Render 实现逻辑 */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mHelper.measuredWidth, mHelper.measuredHeight)
        plogd2(mLogPrefix) { "onMeasure -> measuredWidth = ${mHelper.measuredWidth}, measuredHeight=${mHelper.measuredHeight}" }
    }

    private inline fun checkAvailable() {
        require(!mReleased) {
            "this render is unusable after released."
        }
    }

    init {
        surfaceTextureListener = mSurfaceTextureListener
    }

}