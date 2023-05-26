package unics.player.render

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
import unics.player.controller.UCSPlayerControl
import unics.player.internal.plogd2
import unics.player.internal.plogi2
import unics.player.kernel.UCSPlayer
import unics.player.render.UCSRender.Companion.createShotBitmap
import unics.player.render.UCSRender.ScreenShotCallback
import unics.player.render.UCSRender.SurfaceListener
import java.lang.ref.WeakReference

/**
 * 基于[SurfaceView]实现的[UCSRender]
 *
 * @note 关于[SurfaceView]动态隐藏和显示：经过自己不完全测试，[SurfaceView.setVisibility]在[View.INVISIBLE]和[View.VISIBLE]之间切换没什么问题，
 * 而且某些设备必须直接设置[SurfaceView]该属性才行，通过设置其Parent view group 不可见无法达到效果。使用[View.GONE]总是不符合预期，不知道具体原因
 */
class SurfaceViewRender @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), UCSRender {

    private val mLogPrefix = "[SurfaceViewRender@${this.hashCode()}]"
    private var mReleased: Boolean = false
    private val mHelper: RenderHelper = RenderHelper.create(this)
    private var mSurfaceListener: SurfaceListener? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mPlayerRef: WeakReference<UCSPlayer>? = null
    private val mPlayer: UCSPlayer? get() = mPlayerRef?.get()
    private val mSurfaceHolderCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            plogi2(mLogPrefix) { "surfaceCreated($holder) try invoke player set display,player=${mPlayer} mSurfaceHolder=${mSurfaceHolder}" }
            mSurfaceHolder = holder
            mPlayer?.setDisplay(holder)
            mSurfaceListener?.onSurfaceAvailable(holder.surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            plogi2(mLogPrefix) { "surfaceChanged($holder,$format,$w,$h)" }
            if (holder != mSurfaceHolder) {
                plogi2(mLogPrefix) { "surfaceChanged -> surface holder not same,try set display to player(=$mPlayer)." }
                mSurfaceHolder = holder
                mPlayer?.setDisplay(holder)
                mSurfaceListener?.onSurfaceUpdated(holder.surface)
            } else {
                mSurfaceListener?.onSurfaceSizeChanged(holder.surface, width, height)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            plogi2(mLogPrefix) { "surfaceDestroyed($holder)" }
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null
            mPlayer?.setDisplay(null)
            mSurfaceListener?.onSurfaceDestroyed(holder.surface)
        }
    }

    override fun bindPlayer(player: UCSPlayer?) {
        plogi2(mLogPrefix) { "bindPlayer" }
        checkAvailable()
        val prvPlayer = mPlayerRef?.get()
        if (player == null) {
            plogi2(mLogPrefix) { "bindPlayer -> player is null,unbind previous player." }
            //相当于解除绑定
            val sHolder = mSurfaceHolder
            //如果之前的播放器不为空，并且当前holder可用，则先解除holder引用，再解除player的引用
            if (prvPlayer != null && sHolder != null) {
                plogi2(mLogPrefix) { "bindPlayer -> previous player is not null and surface holder is available,set prv player display holder null." }
                prvPlayer.setDisplay(null)
            }
            mPlayerRef = null
            return
        }
        mPlayerRef = WeakReference(player)
        //当前SurfaceHolder不为空，则说明是重用render
        mSurfaceHolder?.let {
            if (prvPlayer != player) {
                plogi2(mLogPrefix) { "bindPlayer-> player param and previous player ref are not null ,and they are not the same,set prv player display holder null." }
                prvPlayer?.setDisplay(null)
            }
            plogi2(mLogPrefix) { "bindPlayer-> surface holder is available ,attach to player directly." }
            player.setDisplay(it)
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        plogi2(mLogPrefix) { "setVideoSize(videoWidth=$videoWidth,videoHeight=$videoHeight)" }
        checkAvailable()
        mHelper.setVideoSize(videoWidth, videoHeight)
    }

    override fun setSurfaceListener(listener: SurfaceListener?) {
        checkAvailable()
        mSurfaceListener = listener
    }

    override fun setAspectRatioType(aspectRatioType: Int) {
        checkAvailable()
        mHelper.setAspectRatioType(aspectRatioType)
    }

    override fun screenshot(highQuality: Boolean, callback: ScreenShotCallback) {
        checkAvailable()
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
            plogi2(mLogPrefix) { " SurfaceView not support screenshot when Build.VERSION.SDK_INT < Build.VERSION_CODES.N" }
        }
    }

    override val view: View = this

    override fun release() {
        //释放之后便不可再使用
        if (mReleased)
            return
        mReleased = true
        holder.removeCallback(mSurfaceHolderCallback)
        mPlayerRef = null
        mSurfaceHolder = null
        mSurfaceListener = null
    }

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
        val surfaceHolder = holder
        surfaceHolder.addCallback(mSurfaceHolderCallback)
        surfaceHolder.setFormat(PixelFormat.RGBA_8888)
//        /**
//         * 解决surface黑屏问题
//         */
//        setZOrderOnTop(true)
//        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
    }

}