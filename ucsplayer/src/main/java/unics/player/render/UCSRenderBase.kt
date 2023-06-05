package unics.player.render

import android.graphics.Bitmap
import androidx.annotation.IntRange
import unics.player.internal.PartialFunc

interface UCSRenderBase {
    /**
     * 设置界面比例（宽比高）模式
     *
     * @param aspectRatioType 比例类型
     */
    fun setAspectRatioType(@unics.player.render.AspectRatioType aspectRatioType: Int)

    /**
     * 设置视频旋转角度
     *
     * @param degree 角度 0-360
     */
    @PartialFunc(message = "TextureView才支持")
    fun setVideoRotation(@IntRange(from = 0, to = 360) degree: Int) {
        //默认不支持镜像旋转;只有TextureView才支持
    }

    /**
     * 设置镜像旋转，暂不支持SurfaceView
     *
     * @param enable
     */
    @PartialFunc(message = "TextureView才支持")
    fun setMirrorRotation(enable: Boolean) {
        //默认不支持镜像旋转;只有TextureView才支持
    }

    /**
     * 设置视频宽高：用于测量控件的尺寸和比例（通常是Player的回调中调用该方法设置）
     *
     * @param videoWidth  宽
     * @param videoHeight 高
     */
    fun setVideoSize(videoWidth: Int, videoHeight: Int)

    /**
     * 截图
     */
    fun screenshot(callback: UCSRender.ScreenShotCallback) {
        screenshot(false, callback)
    }

    /**
     * 截图
     *
     * @param highQuality 是否采用高质量，默认false；
     * 如果设置为true，则[UCSRender.ScreenShotCallback]返回的[Bitmap]采用[Bitmap.Config.ARGB_8888]配置，相反则采用[Bitmap.Config.RGB_565]
     * @param callback    回调
     * @see Bitmap.Config
     */
    @PartialFunc(message = "SurfaceView在Android 7.0（24）版本及以后才支持")
    fun screenshot(highQuality: Boolean, callback: UCSRender.ScreenShotCallback)

}