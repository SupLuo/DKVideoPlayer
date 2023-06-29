package unics.player

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import androidx.annotation.ColorInt
import unics.player.internal.plogw
import unics.player.kernel.UCSPlayerFactory
import unics.player.kernel.UCSPlayer
import unics.player.render.UCSRenderFactory
import unics.player.render.UCSRender
import unics.player.widget.ProgressManager

/**
 * 视频播放器管理器，管理当前正在播放的VideoView，以及播放器配置
 * 你也可以用来保存常驻内存的VideoView，但是要注意通过Application Context创建，
 * 以免内存泄漏
 *
 * todo：考虑VideoView的共享模式（是让videoview基于application context避免内存混淆还是共享player和player的状态）
 */
object UCSPManager {

    /**
     * 是否开启调试模式
     * 影响一些组件的日志输出
     */
    @JvmStatic
    var isDebuggable: Boolean = false

    /**
     * 播放器内核工厂
     */
    @JvmStatic
    var playerFactory: UCSPlayerFactory<*> = UCSPlayerFactory.system()

    /**
     * Render工厂
     */
    @JvmStatic
    var renderFactory: UCSRenderFactory = UCSRenderFactory.preferred(false)

    /**
     * 获取全局进度管理器
     * @see ProgressManager.default 默认的实现，该实现在程序运行期间，会保存相同url的位置
     */
    @JvmStatic
    var progressManager: ProgressManager? = ProgressManager.default()

    /**
     * 用于共享的VideoView（无缝）
     */
    private val mSharedVideoViews = LinkedHashMap<String, UCSVideoView>()

    /**
     * 用于共享的Player（无缝）
     */
    private val mSharedPlayers = LinkedHashMap<String, UCSPlayer?>()

    /**
     * 是否采用焦点模式：用于TV项目采用按键操作,开启此选项会改变部分Controller&controlComponent的操作方式
     */
    @JvmStatic
    var isTelevisionUiMode = false

    /**
     * 是否启用了音频焦点处理；默认开启
     * 即是否开启AudioFocus监听
     */
    @JvmStatic
    var isAudioFocusEnabled = true

    /**
     *  是否启用设备旋转传感器 （用于横竖屏切换，默认不开启）
     */
    @JvmStatic
    var isOrientationSensorEnabled: Boolean = false

    /**
     * 是否适配刘海屏，默认适配
     */
    @JvmStatic
    var isAdaptCutout: Boolean = true

    /**
     * [unics.player.render.TextureViewRender] 是否开启渲染优化；
     * 默认开启
     */
    @JvmStatic
    var isTextureViewRenderOptimizationEnabled = true

    /**
     * 是否采用硬解码：系统播放器只支持硬解码，IJK播放器默认软解；EXO不了解
     */
    @JvmStatic
    var isMediacodec: Boolean = true

    /**
     * 是否在移动网络下直接播放视频
     * 默认true
     */
    @JvmStatic
    var isPlayOnMobileNetwork: Boolean = true

    /**
     * 图像比例模式
     */
    @JvmStatic
    @unics.player.render.AspectRatioType
    var screenAspectRatioType: Int = unics.player.render.AspectRatioType.DEFAULT_SCALE

    /**
     * 默认播放器背景颜色
     */
    @JvmStatic
    @ColorInt
    var defaultPlayerBackgroundColor:Int = Color.BLACK

    /**
     * [UCSRender] 是否重用（即在播放器调用播放或者重新播放的时候，是否重用已有的RenderView：以前的版本是每次都会创建一个新的RenderView）
     */
    @JvmStatic
    var isRenderReusable: Boolean = true

    /**
     * [UCSPlayer]是否重用（即在播放器贼每次使用时，是否重用已有的Player：以前的版本是每次都会创建一个新的播放器）
     * todo：目前播放器重用的情况下，surface以及事件之类的处理没有重新绑定
     * todo:目前该功能支持的不是很好，暂不开放
     */
    @JvmStatic
    var isPlayerKernelReusable: Boolean = false

    /**
     * 创建播放器内核
     *
     * @param context
     * @param customFactory 自定义工厂，如果为null，则使用全局配置的工厂创建
     * @return
     */
    @JvmStatic
    fun createPlayerKernel(context: Context, customFactory: UCSPlayerFactory<*>?): UCSPlayer {
        return (customFactory ?: playerFactory).create(context)
    }

    /**
     * 创建系统播放器
     * @note 该方法预留在此处，用于处理以后处理某些盒子芯片只支持单个播放器实例的情况
     */
    @JvmStatic
    internal fun createMediaPlayer():MediaPlayer{
        return MediaPlayer()
    }

    /**
     * 获取或创建共享的播放器，通常用于播放器共享的情况（比如为了实现界面无缝切换效果）
     *
     * @param context
     * @param tag           共享标记
     * @param customFactory 自定义工厂
     * @return
     */
    fun getOrCreateSharedMediaPlayer(
        context: Context,
        tag: String,
        customFactory: UCSPlayerFactory<*>?
    ): UCSPlayer? {
        return mSharedPlayers.getOrPut(tag) {
            createPlayerKernel(context, customFactory)
        }
    }

    /**
     * 移除共享的播放器
     *
     * @param tag
     */
    fun removeSharedMediaPlayer(tag: String) {
        mSharedPlayers.remove(tag)
    }

    /**
     * 添加VideoView
     *
     * @param tag 相同tag的VideoView只会保存一个，如果tag相同则会release并移除前一个
     */
    @JvmStatic
    fun add(videoView: UCSVideoView, tag: String) {
        if (videoView.context !is Application) {
            plogw {
                "The Context of this VideoView is not an Application Context,you must remove it after release,or it will lead to memory leek."
            }
        }
        val old = get(tag)
        if (old != null) {
            old.release()
            remove(tag)
        }
        mSharedVideoViews[tag] = videoView
    }

    fun get(tag: String): UCSVideoView? {
        return mSharedVideoViews[tag]
    }

    fun remove(tag: String) {
        mSharedVideoViews.remove(tag)
    }

    fun removeAll() {
        mSharedVideoViews.clear()
    }

    /**
     * 释放掉和tag关联的VideoView，并将其从VideoViewManager中移除
     */
    @JvmOverloads
    fun releaseByTag(tag: String, isRemove: Boolean = true) {
        val videoView = get(tag)
        if (videoView != null) {
            videoView.release()
            if (isRemove) {
                remove(tag)
            }
        }
    }

    fun onBackPress(tag: String): Boolean {
        val videoView = get(tag) ?: return false
        return videoView.onBackPressed()
    }

}