package droid.unicstar.player.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import droid.unicstar.player.logd
import droid.unicstar.player.UCSPlayerManager
import droid.unicstar.player.player.UCSPlayer
import droid.unicstar.player.render.UCSRender.Companion.createShotBitmap
import droid.unicstar.player.render.UCSRender.ScreenShotCallback
import droid.unicstar.player.render.UCSRender.SurfaceListener
import droid.unicstar.player.render.internal.RenderHelper
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

    private val mHelper: RenderHelper = RenderHelper.create(this)
    private var mPlayerRef: WeakReference<UCSPlayer>? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurface: Surface? = null
    private var mSurfaceListener: SurfaceListener? = null
    private val mEnableRenderOptimization: Boolean = UCSPlayerManager.isTextureViewRenderOptimizationEnabled
    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            logd(
                "TextureViewRender",
                "onSurfaceTextureAvailable $surfaceTexture $width $height ${mPlayerRef?.get()}"
            )
            if (mEnableRenderOptimization) {
                //开启渲染优化
                if (mSurfaceTexture == null) {
                    mSurfaceTexture = surfaceTexture
                    mSurface = Surface(surfaceTexture)
                    bindSurfaceToMediaPlayer(mSurface!!)
                } else {
                    //在开启优化的情况下，使用最开始的那个渲染器
                    setSurfaceTexture(mSurfaceTexture!!)
                }
            } else {
                mSurface = Surface(surfaceTexture)
                bindSurfaceToMediaPlayer(mSurface!!)
            }
            notifySurfaceAvailable(mSurface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            logd("TextureViewRender", "onSurfaceTextureSizeChanged $surfaceTexture $width $height")
            mSurfaceListener?.onSurfaceSizeChanged(mSurface, width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            logd("TextureViewRender", "onSurfaceTextureDestroyed $surfaceTexture")
            //清空释放
            mSurfaceListener?.onSurfaceDestroyed(mSurface)
            return if (mEnableRenderOptimization) {
                //如果开启了渲染优化，那mSurfaceTexture通常情况不可能为null（在onSurfaceTextureAvailable初次回调的时候被赋值了），
                // 所以这里通常返回的是false，返回值false会告诉父类不要释放SurfaceTexture
                mSurfaceTexture == null
            } else {
                true
            }
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            if (mEnableRenderOptimization && mSurfaceTexture == null) {
                //用于修正开启渲染优化的情况下，mSurfaceTexture为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法
                mSurfaceTexture = surface
                mSurface = Surface(surface)
                bindSurfaceToMediaPlayer(mSurface!!)
                notifySurfaceAvailable(mSurface, width, height)
            } else if (mSurface == null) {
                //用于修正未开启渲染优化的情况下，mSurface为空的异常情况：据说是因为存在机型不回调onSurfaceTextureAvailable而只回调此方法
                mSurface = Surface(surfaceTexture)
                bindSurfaceToMediaPlayer(mSurface!!)
                notifySurfaceAvailable(mSurface, width, height)
            } else {
                //onSurfaceTextureUpdated会在SurfaceTexture.updateTexImage()的时候回调该方法，因此只要图像更新，都会调用本方法，因此在这个方法中不适合做什么处理
//                Log.d("TextureView", "onSurfaceTextureUpdated $surface")
//                mSurfaceListener?.onSurfaceUpdated(mSurface)
            }
        }

        private fun bindSurfaceToMediaPlayer(surface: Surface) {
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
    override fun bindPlayer(player: UCSPlayer?) {
        if(player == null){
            mPlayerRef = null
            return
        }
        mPlayerRef = WeakReference(player)
        //当前surface不为空，则说明是surface重用
        mSurface?.let {
            player.setSurface(it)
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        mHelper.setVideoSize(videoWidth, videoHeight)
    }

    /*************START Render 实现逻辑 */

    override val view: View = this

    override fun setSurfaceListener(listener: SurfaceListener?) {
        mSurfaceListener = listener
    }

    override fun screenshot(highQuality: Boolean, callback: ScreenShotCallback) {
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
        mHelper.setAspectRatioType(aspectRatioType)
    }

    override fun setVideoRotation(degree: Int) {
        mHelper.setVideoRotation(degree)
    }

    override fun setMirrorRotation(enable: Boolean) {
        mHelper.setMirrorRotation(enable)
    }

    override fun release() {
        mPlayerRef = null
        mSurface?.release()
        mSurfaceTexture?.release()
    }

    /*************END Render 实现逻辑 */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mHelper.measuredWidth, mHelper.measuredHeight)
    }

    init {
        surfaceTextureListener = mSurfaceTextureListener
    }

}