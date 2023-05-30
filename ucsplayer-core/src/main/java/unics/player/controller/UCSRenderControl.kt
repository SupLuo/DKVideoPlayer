package unics.player.controller

import androidx.annotation.IntRange
import unics.player.internal.PartialFunc
import unics.player.render.UCSRender
import unics.player.render.UCSRenderFactory

/**
 * Render控制层提供的功能；具体由[droid.unicstar.player.UCSRenderProxy]实现
 */
interface UCSRenderControl {

    /**
     * 设置Render是否重用：即每次播放是否重新创建一个新的图层,默认[xyz.doikki.videoplayer.DKManager.isRenderReusable]
     */
    fun setRenderReusable(reusable: Boolean)

    /**
     * 自定义RenderView，继承[UCSRenderFactory]实现自己的RenderView
     */
    fun setRenderViewFactory(factory: UCSRenderFactory?)

    /**
     * 设置界面比例（宽比高）模式
     *
     * @param aspectRatioType 类型
     */
    fun setAspectRatioType(@unics.player.render.AspectRatioType aspectRatioType: Int)

    /**
     * 设置播放控件旋转角度
     *
     * @param degree 角度 0-360
     */
    @PartialFunc(message = "TextureView才支持")
    fun setVideoRotation(@IntRange(from = 0, to = 360) degree: Int)

    /**
     * 设置镜像旋转
     */
    fun setMirrorRotation(enable: Boolean)

    /**
     * 截图
     *
     * @see UCSRender#screenshot(boolean, UNSRender.ScreenShotCallback)
     */
    fun screenshot(callback: UCSRender.ScreenShotCallback) {
        screenshot(false, callback)
    }

    fun screenshot(highQuality: Boolean, callback: UCSRender.ScreenShotCallback)

    /**
     * 设置视频大小
     */
    fun setVideoSize(videoWidth: Int, videoHeight: Int)

    /**
     * 获取视频宽高,其中width: IntArray[0], height: IntArray[1]
     */
     fun getVideoSize(): IntArray

}