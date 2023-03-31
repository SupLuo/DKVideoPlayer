package droid.unicstar.videoplayer.render

import android.content.Context

/**
 * todo 本身可以采用lambda实现，但是不利于调试时通过classname获取render的名字
 */
internal class SurfaceViewRenderFactory : RenderFactory {

    override fun create(context: Context): Render {
        return SurfaceViewRender(context)
    }

    override fun equals(other: Any?): Boolean {
        if (other is SurfaceViewRenderFactory)
            return true
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return this.javaClass.hashCode()
    }

}