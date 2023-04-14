package droid.unicstar.videoplayer.render

import android.content.Context
import android.os.Build
import droid.unicstar.videoplayer.render.UNSRenderFactory.Companion.textureViewRenderFactory
import xyz.doikki.videoplayer.DKManager

/**
 * 此接口用于扩展自己的渲染View。使用方法如下：
 * 1.继承IRenderView实现自己的渲染View。
 * 2.重写createRenderView返回步骤1的渲染View。
 * 3.通过[DKManager.renderFactory] 设置步骤2的实例
 * 可参考[TextureViewRenderFactory]和[textureViewRenderFactory]的实现。
 */
fun interface UNSRenderFactory {

    fun create(context: Context): UNSRender

    companion object {

        @JvmStatic
        val DEFAULT: UNSRenderFactory =
            if (Build.VERSION.SDK_INT < 21) surfaceViewRenderFactory() else textureViewRenderFactory()

        @JvmStatic
        fun textureViewRenderFactory(): UNSRenderFactory {
            return TextureViewRenderFactory()
        }

        @JvmStatic
        fun surfaceViewRenderFactory(): UNSRenderFactory {
            return SurfaceViewRenderFactory()
        }
    }
}