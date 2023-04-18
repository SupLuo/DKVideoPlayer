package droid.unicstar.player.render

import android.content.Context
import android.os.Build
import droid.unicstar.player.render.UCSRenderFactory.Companion.textureViewRenderFactory
import droid.unicstar.player.UCSPlayerManager

/**
 * 此接口用于扩展自己的渲染View。使用方法如下：
 * 1.继承IRenderView实现自己的渲染View。
 * 2.重写createRenderView返回步骤1的渲染View。
 * 3.通过[UCSPlayerManager.renderFactory] 设置步骤2的实例
 * 可参考[TextureViewRenderFactory]和[textureViewRenderFactory]的实现。
 */
fun interface UCSRenderFactory {

    fun create(context: Context): UCSRender

    companion object {

        @JvmStatic
        val DEFAULT: UCSRenderFactory =
            if (Build.VERSION.SDK_INT < 21) surfaceViewRenderFactory() else textureViewRenderFactory()

        @JvmStatic
        fun textureViewRenderFactory(): UCSRenderFactory {
            return TextureViewRenderFactory()
        }

        @JvmStatic
        fun surfaceViewRenderFactory(): UCSRenderFactory {
            return SurfaceViewRenderFactory()
        }
    }
}