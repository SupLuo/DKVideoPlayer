package droid.unicstar.videoplayer.render

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import droid.unicstar.videoplayer.logd
import droid.unicstar.videoplayer.logw
import droid.unicstar.videoplayer.render.UNSRender.Companion.createShotBitmap
import droid.unicstar.videoplayer.render.UNSRender.ScreenShotCallback
import droid.unicstar.videoplayer.render.UNSRender.SurfaceListener
import droid.unicstar.videoplayer.render.internal.RenderViewProxy
import droid.unicstar.videoplayer.player.UNSPlayer
import java.lang.ref.WeakReference

/**
 * 基于[SurfaceView]实现的[UNSRender]
 *
 * @note 关于[SurfaceView]动态隐藏和显示：经过自己不完全测试，[SurfaceView.setVisibility]在[View.INVISIBLE]和[View.VISIBLE]之间切换没什么问题，
 * 而且某些设备必须直接设置[SurfaceView]该属性才行，通过设置其Parent view group 不可见无法达到效果。使用[View.GONE]总是不符合预期，不知道具体原因
 */
class SurfaceViewRender @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), UNSRender {

    private val mProxy: RenderViewProxy = RenderViewProxy.new(this)
    private var mSurfaceListener: SurfaceListener? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mPlayerRef: WeakReference<UNSPlayer>? = null
    private val mPlayer: UNSPlayer? get() = mPlayerRef?.get()
    private val mSurfaceHolderCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            logd(
                "SurfaceViewRender",
                "player=${mPlayer} ${this@SurfaceViewRender.hashCode()} mSurfaceHolder=${mSurfaceHolder} surfaceCreated($holder)"
            )
            mSurfaceHolder = holder
            mPlayer?.setDisplay(holder)
            mSurfaceListener?.onSurfaceAvailable(holder.surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            logd(
                "SurfaceViewRender",
                "player=${mPlayer} ${this@SurfaceViewRender.hashCode()} mSurfaceHolder=${mSurfaceHolder} surfaceChanged($holder,$format,$w,$h)"
            )
            mPlayer?.setDisplay(holder)
            if (holder != mSurfaceHolder) {
                mSurfaceHolder = holder
                mPlayer?.setDisplay(holder)
                mSurfaceListener?.onSurfaceUpdated(holder.surface)
            } else {
                mSurfaceListener?.onSurfaceSizeChanged(holder.surface, width, height)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            logd(
                "SurfaceViewRender",
                "player=${mPlayer}  ${this@SurfaceViewRender.hashCode()} mSurfaceHolder=${mSurfaceHolder} surfaceDestroyed($holder)"
            )
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null
            mPlayer?.setDisplay(null)
            mSurfaceListener?.onSurfaceDestroyed(holder.surface)
        }
    }

    override fun bindPlayer(player: UNSPlayer?) {
        if (player == null) {
            mPlayerRef = null
            return
        }
        mPlayerRef = WeakReference(player)
        //当前SurfaceHolder不为空，则说明是重用render
        mSurfaceHolder?.let {
            player.setDisplay(it)
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        mProxy.setVideoSize(videoWidth, videoHeight)
    }

    override fun setSurfaceListener(listener: SurfaceListener?) {
        mSurfaceListener = listener
    }

    override fun setAspectRatioType(aspectRatioType: Int) {
        mProxy.setAspectRatioType(aspectRatioType)
    }

    override fun screenshot(highQuality: Boolean, callback: ScreenShotCallback) {
        if (Build.VERSION.SDK_INT >= 24) {
            val bmp = createShotBitmap(this, highQuality)
            val handlerThread = HandlerThread("PixelCopier")
            handlerThread.start()
            PixelCopy.request(this, bmp, { copyResult: Int ->
                try {
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback.onScreenShotResult(bmp)
                    }
                    handlerThread.quitSafely()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    if (!bmp.isRecycled) bmp.recycle()
                    callback.onScreenShotResult(null)
                }
            }, Handler())
        } else {
            callback.onScreenShotResult(null)
            logw(
                "SurfaceViewRender",
                "SurfaceView not support screenshot when Build.VERSION.SDK_INT < Build.VERSION_CODES.N"
            )
        }
    }

    override val view: View = this

    override fun release() {
        holder.removeCallback(mSurfaceHolderCallback)
        mPlayerRef = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mProxy.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mProxy.measuredWidth, mProxy.measuredHeight)
    }

    init {
        val surfaceHolder = holder
        surfaceHolder.addCallback(mSurfaceHolderCallback)
        surfaceHolder.setFormat(PixelFormat.RGBA_8888)
    }
}