package unics.player.render


/**
 * Render控制层提供的功能；具体由[RenderProxy]实现
 */
interface UCSRenderControl : UCSRenderBase {

    /**
     * 设置Render是否重用：即每次播放是否重新创建一个新的图层,默认[unics.player.UCSPManager.isRenderReusable]
     */
    fun setRenderReusable(reusable: Boolean)

    /**
     * 自定义RenderView，继承[UCSRenderFactory]实现自己的RenderView
     */
    fun setRenderViewFactory(factory: UCSRenderFactory?)

//    /**
//     * 获取视频宽高,其中width: IntArray[0], height: IntArray[1]
//     */
//    fun getVideoSize(): IntArray

}