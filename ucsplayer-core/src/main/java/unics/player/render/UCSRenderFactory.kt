package unics.player.render

import android.content.Context
import unics.player.UCSPlayerManager
import unics.player.internal.sdkInt
import unics.player.render.UCSRenderFactory.Companion.textureView

/**
 * 此接口用于扩展自己的渲染View。使用方法如下：
 * 1.继承IRenderView实现自己的渲染View。
 * 2.重写createRenderView返回步骤1的渲染View。
 * 3.通过[UCSPlayerManager.renderFactory] 设置步骤2的实例
 * 可参考[TextureViewRenderFactory]和[textureView]的实现。
 */
fun interface UCSRenderFactory {

    fun create(context: Context): UCSRender

    companion object {

        /**
         * 推荐的render工厂
         * @param isTelevision 是否在TV/盒子上运行（更多的是说盒子），如果在盒子上运行，surface问题少些
         */
        @JvmStatic
        fun preferred(isTelevision: Boolean): UCSRenderFactory {
            return if (!isTelevision && sdkInt >= 21) {
                textureView()
            } else {
                surfaceView()
            }
        }

        @JvmStatic
        fun textureView(): UCSRenderFactory {
            return TextureViewRenderFactory()
        }

        @JvmStatic
        fun surfaceView(): UCSRenderFactory {
            return SurfaceViewRenderFactory()
        }
    }
}