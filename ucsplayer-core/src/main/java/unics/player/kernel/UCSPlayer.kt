package unics.player.kernel

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.IntDef
import unics.player.internal.PartialFunc

/**
 * 抽象的播放器，继承此接口扩展自己的播放器
 *
 * @note 本类设计：
 * 本类的设计主要作为播放器的适配器接口，消除各种播放器内核之间的差异，提供统一的操作方式
 * 本类的职责应该完全定位在播放器的“能力”上，因此只考虑与播放相关的逻辑（不包括UI层面）

 * create by luochao on 2022/9/16. 调整部分代码及结构
 * @see BaseUCSPlayer
 */
interface UCSPlayer : UCSPlayerBase {

    companion object {

        /**
         * 创建一个播放器：创建的是一个播放器代理，支持的功能更多
         */
        @JvmStatic
        fun create(context: Context): PlayerProxy {
            return PlayerProxy(context)
        }

        /**
         * 视频/音频开始渲染
         */
        const val MEDIA_INFO_VIDEO_RENDERING_START = MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START

        /**
         * 视频/音频已准备开始渲染:通常用于在prepared状态之后调用了[start]方法，播放器即将进入[STATE_PLAYING]
         * (兼容状态，用于解决纯音频无法判断什么时候开始播放的)
         */
        const val MEDIA_INFO_RENDERING_PREPARED = 20001

        /**
         * 缓冲开始
         */
        const val MEDIA_INFO_BUFFERING_START = MediaPlayer.MEDIA_INFO_BUFFERING_START

        /**
         * 缓冲结束
         */
        const val MEDIA_INFO_BUFFERING_END = MediaPlayer.MEDIA_INFO_BUFFERING_END

        /**
         * 视频旋转信息
         */
        const val MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001

        /**
         * 未知错误
         */
        const val MEDIA_ERROR_UNKNOWN = MediaPlayer.MEDIA_ERROR_UNKNOWN

        /**
         * 播放出错
         */
        const val STATE_ERROR = -1

        /**
         * 闲置中
         */
        const val STATE_IDLE = 0

        /**
         * 准备中：处于已设置了播放数据源，但是播放器还未回调[UCSPlayer.EventListener.onPrepared]
         */
        const val STATE_PREPARING = 1

        /**
         * 已就绪
         */
        const val STATE_PREPARED = 2

        /**
         * 已就绪但终止状态
         * 播放过程中停止继续播放：比如手机不允许在手机流量的时候进行播放（此时播放器处于已就绪未播放中状态）
         */
        const val STATE_PREPARED_BUT_ABORT = 8

        /**
         * 播放中
         */
        const val STATE_PLAYING = 3

        /**
         * 暂停中
         */
        const val STATE_PAUSED = 4

        /**
         * 播放结束
         */
        const val STATE_PLAYBACK_COMPLETED = 5

        /**
         * 缓冲中
         */
        const val STATE_BUFFERING = 6

        /**
         * 缓冲结束
         */
        const val STATE_BUFFERED = 7

    }

    /**
     * 播放器状态
     */
    @IntDef(
        //出错
        STATE_ERROR,
        //闲置
        STATE_IDLE,
        //准备数据源中：setDatasource与onPrepared之间
        STATE_PREPARING,
        //数据源已准备：onPrepared回调
        STATE_PREPARED,
        //开始播放：调用start()之后
        STATE_PLAYING,
        //暂停
        STATE_PAUSED,
        //播放结束
        STATE_PLAYBACK_COMPLETED,
        //缓冲中
        STATE_BUFFERING,
        //缓冲结束
        STATE_BUFFERED,
        //已准备但因为用户设置不允许移动网络播放而中断：onPrepared回调之后并没有调用start
        STATE_PREPARED_BUT_ABORT
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class PlayState

    /**
     * 事件监听器
     */
    interface EventListener {

        /**
         * 播放就绪
         */
        fun onPrepared() {}

        /**
         * 播放信息
         */
        fun onInfo(what: Int, extra: Int) {}

        /**
         * 视频大小发生变化
         */
        fun onVideoSizeChanged(width: Int, height: Int) {}

        /**
         * 播放完成
         */
        fun onCompletion() {}

        /**
         * 播放错误
         */
        fun onError(e: Throwable) {}
    }

    /**
     * 播放器状态发生变化监听
     */
    fun interface OnPlayStateChangeListener {
        /**
         * 播放器播放状态发生了变化
         *
         * @param playState
         */
        fun onPlayStateChanged(@PlayState playState: Int)

    }

    /**
     * 设置播放地址
     *
     * @param path    播放地址
     */
    fun setDataSource(context: Context, path: String) = setDataSource(context, path, null)

    /**
     * 设置播放地址
     *
     * @param path    播放地址
     * @param headers 播放地址请求头
     */
    fun setDataSource(context: Context, path: String, headers: Map<String, String>?) =
        setDataSource(context, Uri.parse(path), headers)

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     */
    fun setDataSource(context: Context, uri: Uri) = setDataSource(context, uri, null)

    /**
     * 设置播放地址
     *
     * @param uri    the Content URI of the data you want to play
     * @param headers 播放地址请求头
     */
    fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?)

    /**
     * 用于播放raw和asset里面的视频文件
     */
    fun setDataSource(fd: AssetFileDescriptor)

    /**
     * 异步准备
     */
    fun prepareAsync()

    /**
     * 停止
     * 即让暂停或者正在进行的播放停止，接下来想继续播放的话，得重新设置数据源并调用prepareAsync
     */
    fun stop()

    /**
     * 重置播放器
     */
    fun reset()

    /**
     * 释放播放器
     * 释放之后此播放器就不能再被使用
     */
    fun release()

    /**
     * 设置播放器事件监听
     */
    fun setEventListener(eventListener: EventListener?)

    /**
     * 设置解码方式
     * @param enable true:硬解 false 软解
     */
    @PartialFunc(message = "系统播放器无效，只支持硬解；Ijk支持软硬切换；Exo暂未了解")
    fun setMediacodec(enable: Boolean) {
    }

}