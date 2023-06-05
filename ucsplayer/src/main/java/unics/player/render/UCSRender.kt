package unics.player.render

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.Surface
import android.view.View
import androidx.annotation.IntRange
import unics.player.internal.PartialFunc
import unics.player.kernel.UCSPlayerBase

interface UCSRender : UCSRenderBase{

    companion object {

        /**
         * 创建截图的Bitmap容器
         */
        @JvmStatic
        fun createShotBitmap(
            context: Context,
            width: Int,
            height: Int,
            highQuality: Boolean
        ): Bitmap {
            val config = if (highQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            return if (Build.VERSION.SDK_INT >= 17) {
                Bitmap.createBitmap(
                    context.resources.displayMetrics,
                    width, height, config
                )
            } else {
                Bitmap.createBitmap(width, height, config)
            }
        }

        /**
         * 创建截图的Bitmap容器
         */
        @JvmStatic
        fun createShotBitmap(render: UCSRender, highQuality: Boolean): Bitmap {
            val view = render.view
            return createShotBitmap(view!!.context, view.width, view.height, highQuality)
        }

    }

    interface SurfaceListener {
        /**
         * Invoked when a [UCSRender]'s Surface is ready for use.
         *
         * @param surface The surface returned by getSurfaceTexture()
         */
        fun onSurfaceAvailable(surface: Surface?)

        /**
         * Invoked when the [SurfaceTexture]'s buffers size changed.
         *
         * @param surface The surface returned by
         * [android.view.TextureView.getSurfaceTexture]
         * @param width   The new width of the surface
         * @param height  The new height of the surface
         */
        fun onSurfaceSizeChanged(surface: Surface?, width: Int, height: Int)
        fun onSurfaceDestroyed(surface: Surface?): Boolean
        fun onSurfaceUpdated(surface: Surface?)
    }

    /**
     * 截图回调
     */
    fun interface ScreenShotCallback {
        /**
         * 截图结果
         *
         * @param bmp
         */
        fun onScreenShotResult(bmp: Bitmap?)
    }

    /**
     * 获取真实的RenderView:用于挂在view tree上
     */
    val view: View?

    /**
     * 绑定播放器
     * @param player 播放器，如果为null，则会解除之前的播放器持有
     */
    fun bindPlayer(player: UCSPlayerBase?)

    /**
     * 释放资源
     */
    fun release()

    /**************以下为Render的功能能力 */
    /**
     * 设置Surface监听
     *
     * @param listener
     */
    fun setSurfaceListener(listener: SurfaceListener?)

}