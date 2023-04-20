package xyz.doikki.videoplayer.ijk

import android.media.MediaCodec
import android.os.Bundle
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * IJK参数相关配置参考文档：https://github.com/Bilibili/ijkplayer/blob/master/ijkmedia/ijkplayer/ff_ffplay_options.h
 * 优化和解决方案文档参考：https://www.cnblogs.com/marklove/articles/10608812.html
 *
 *
一、ijkplayer播放主要流程：
1、根据链接的schema找到对应的URLProtocol。
如Http的链接，对应libavformat/http.c
而http的请求后续会转换成Tcp的协议，对应libavformat/tcp.c
2、进行DNS解析ip地址，并且解析完后进行缓存，以便下次复用
3、从链路中读取数据到Buffer
有可能从tcp链路，也有可能从磁盘链路
TCP链路则会需要等待三次握手的时间
4、读取Buffer进行文件类型的probe
探测文件格式，判断是mp4，flv等等
5、读取Buffer的头部信息进行解析
解析文件头部，判断是否为该格式文件，如果失败则返回错误
6、解析audio，video，subtitle流
根据文件信息找到多媒体流
优先使用H264的视频流
7、根据流信息找到解码器
8、开启各个线程开始对各个流进行解码成packet
9、同步到read_thread线程后，装入packetQueue中
10、在video_refresh_thread线程中，读取packetQueue中的包，进行时钟同步
11、开始绘制视频，播放音频内容

二、ijkplayer优化方向
2.1 网络链路优化
IP直连：减少dns
减少随机值：提高CDN缓存资源命中率
2.2 文件探测&头部读取优化
控制文件探测大小：probesize参数
控制分析时长：analyzeduration参数
去掉循环滤波：skip_loop_filter参数
2.3 buffer优化
直接刷新数据包：flush_packets
去掉packet-buffering：packet-buffering
2.4 解码优化
是否可以不解析subtitle、audio

>当probesize和analyzeduration过小时，可能会造成预读数据不足，无法解析出码流信息，从而导致播放失败、无音频或无视频的情况。
所以，在服务端对视频格式进行标准化转码，从而确定视频格式，进而再去推算 avformat_find_stream_info 分析码流信息所兼容的最小的 probesize 和 analyzeduration，
就能在保证播放成功率的情况下最大限度地区优化首屏秒开

 */

//todo 梳理该文档：https://www.jianshu.com/p/496257563f69

/**
 * 软解推荐的配置
 * @param frameDrop 跳帧
 */
fun IjkMediaPlayer.applySoftDecodingPreferredOptions(frameDrop: Long = 1) {
    setFrameDrop(frameDrop)//设置跳帧
    applySoftDecoding()
    setMaxBufferSize(5 * 1024 * 1024)//设置最大缓冲区大小 5Mkb
    setMaxFps(24) //设置最大帧率
    setSkipLoopFilter(false)
    setEnableReconnect(true)

    //以下是没太搞懂的配置
    setAnalyzeMaxDuration(100) // 设置最长分析时长
    setFlushPackets(true)
    setPacketBuffering(false)// 禁用暂停输出直到停止后读取足够的数据包
}


/**
 * 秒开推荐配置
 * @see setAnalyzeMaxDuration
 * @see setAnalyzeDuration
 * @see setProbeSize
 * @see setFindStreamInfo
 * 以上四个方法应该是相关有关系的
 */
fun IjkMediaPlayer.applyFastStartPreferredOptions() {
    setAnalyzeMaxDuration(100) // 设置最长分析时长
    setAnalyzeDuration(1)
    setProbeSize(10 * 1024)
    setFlushPackets(true)
    setPacketBuffering(false)// 禁用暂停输出直到停止后读取足够的数据包
    setFrameDrop(5)//设置跳帧
}

/**
 * 直播首选配置说明
 */
fun IjkMediaPlayer.applyLivePreferredOptions() {
//    7、快速起直播流：
//    直播技术总结（五）如何快速起播直播流- http://blog.csdn.net/hejjunlin/article/details/72860470
//    这里优化后者，主要修改两个参数，一个是probesize，一个是analyzeduration，分别用来控制其读取的数据量大小和时长。减少 probesize 和 analyzeduration 可以降低avformat_find_stream_info的函数耗时，达到起播快

}

/**
 * Rtsp配置优化
 * 参考来源 （第21条）https://www.jianshu.com/p/220b00d00deb
 */
fun IjkMediaPlayer.applyRtspPreferredOptions() {

//rtsp设置 https://ffmpeg.org/ffmpeg-protocols.html#rtsp
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp");

    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_media_types", "video"); //根据媒体类型来配置
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 20000);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "buffer_size", 1316);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 1);  // 无限读
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
//  关闭播放器缓冲，这个必须关闭，否则会出现播放一段时间后，一直卡主，控制台打印 FFP_MSG_BUFFERING_START
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L);
}

/**
 * 设置解码器,ijk默认使用软解
 * @param hardware true-硬解码；false-软解
 * @note 高分辨率开启硬解码，不支持的话会自动切换到软解，就算开启mediacodec，如果设备不支持，显示的解码器也是avcodec软解；资料来源： https://www.cnblogs.com/renhui/p/6420140.html
 */
inline fun IjkMediaPlayer.setMediacodec(hardware: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", hardware.ijkBoolean)
}

/**
 * 使用软解
 */
inline fun IjkMediaPlayer.applySoftDecoding() {
    setMediacodec(false)
}

/**
 * 使用硬解
 * @param autoRotate 是否根据meta信息自动旋转，默认开启；
 * MediaCodec: auto rotate frame depending on meta;
 *
 * @param handleResolutionChange 自动处理分辨率变化；
 * MediaCodec: handle resolution change automatically
 */
inline fun IjkMediaPlayer.applyHardDecoding(
    autoRotate: Boolean = true,
    handleResolutionChange: Boolean = true
) {
    setMediacodec(true)
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", autoRotate.ijkBoolean)
    setOption(
        IjkMediaPlayer.OPT_CATEGORY_PLAYER,
        "mediacodec-handle-resolution-change",
        handleResolutionChange.ijkBoolean
    )
}

/**
 * 设置精准seek模式
 * @note ijkplyer 在播放部分视频时，调用seekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题（GOP导致的），视频压缩比较高，而seek只支持关键帧，出现这个情况就是原始的视频文件中i帧比较少，播放器会在拖动的位置找最近的关键帧。
 * @note 开启精准seek，会很大程度减少seekTo的问题，但据说仍然不可避免
 * 参考资料：https://www.cnblogs.com/renhui/p/6420140.html
 */
inline fun IjkMediaPlayer.setAccurateSeek(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", enable.ijkBoolean)
}

/**
 * 设置启用快速Seek
 * @note 避免文件过大，seek位置过远导致loading时间过长的问题
 * eg.一个3个多少小时的音频文件，开始播放几秒中，然后拖动到2小时左右的时间，要loading 10分钟
 */
inline fun IjkMediaPlayer.setFastSeek() {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");//设置seekTo能够快速seek到指定位置并播放
}

/**
 * 1: 设置变调（改变声调）,ijk默认不开启;参考链接：https://www.cnblogs.com/renhui/p/6510872.html
 * @note 通常用于倍速变调的场景（B站的倍速变调效果）
 */
inline fun IjkMediaPlayer.setSoundTouch(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", enable.ijkBoolean)
}

/**
 * 跳帧处理：在视频帧处理不过来的时候丢弃一些帧达到(画面和声音)同步的效果，保证播放流畅
 * @note 软解使用的情况下，尽量开启掉帧处理
 */
inline fun IjkMediaPlayer.setFrameDrop(count: Long = 5) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", count)
}

/**
 * 2:设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
 * 考虑性能的情况下建议关闭；
 * @note 设置为false，跳过循环滤波
 */
fun IjkMediaPlayer.setSkipLoopFilter(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", if (enable) 0L else 48L)
}

/**
 *  3:设置播放前的最大探测时间
 * @see setAnalyzeDuration
 * @see setProbeSize
 * @see setMinFrames
 * 该方法涉及预读配置，与前面这些方法应该有关联
 * todo 搞清概念
 */
fun IjkMediaPlayer.setAnalyzeMaxDuration(durationMsec: Long = 100) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", durationMsec)
}

/**
 * 4:设置播放前的探测时间 1,达到首屏秒开效果
 * @see setAnalyzeMaxDuration
 * @see setProbeSize
 * @see setMinFrames
 * todo 搞清概念
 * 该方法涉及预读配置，与前面这些方法应该有关联
 */
inline fun IjkMediaPlayer.setAnalyzeDuration(time: Long = 1) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", time)
}

/**
 * 5:播放前的探测Size，默认是1M, 改小一点会出画面更快
 * @param bytes
 *
 * @see setAnalyzeDuration
 * @see setAnalyzeMaxDuration
 * @see setMinFrames
 * todo 搞清概念
 * 该方法涉及预读配置，与前面这些方法应该有关联
 */
inline fun IjkMediaPlayer.setProbeSize(bytes: Long = 1024 * 10) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", bytes)
}

/**
 * 读取多少帧之后停止预读取
 * minimal frames to stop pre-reading
 * @see setAnalyzeDuration
 * @see setAnalyzeMaxDuration
 * @see setProbeSize
 * todo 搞清概念
 * 该方法涉及预读配置，与前面这些方法应该有关联
 */
inline fun IjkMediaPlayer.setMinFrames(frames: Long) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", frames)
}

/**
 * 是否查询stream_info,ijk默认设置为true
 * @param enable true:表示要查找streamInfo，false表示不查找直接使用
 */
inline fun IjkMediaPlayer.setFindStreamInfo(enable: Boolean) {
    // 不查询stream_info，直接使用
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "find_stream_info", enable.ijkBoolean)
}

/**
 * 6:每处理一个packet之后刷新io上下文
 * @param enable true：开启该功能，false则关闭
 * @note 通过立即清理数据包来减少等待时长
 * @note 未在git上找到相关配置
 */
inline fun IjkMediaPlayer.setFlushPackets(enable: Boolean = true) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", enable.ijkBoolean)
}

/**
 * 暂停输出，直到在停止后读取了足够的数据包；ijk默认开启
 * pause output until enough packets have been read after stalling
 * @param enable true：开启该功能，false则关闭
 * @note 禁用的话相当于有数据就会输出，但是据说禁用之后就可能带来卡顿掉帧的问题（个人感觉可能会在某些情况导致频繁卡顿的问题：因为不会暂停等待足够的包数据，所以如果网络不好，数据输入不够快，那么会导致卡顿越频繁）
 */
inline fun IjkMediaPlayer.setPacketBuffering(enable: Boolean = true) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", enable.ijkBoolean)
}

/**
 * 是否清空dns缓存
 * @note 可以用于以下场景开启：比如http与https域名共用，eg http://www.baidu.com  and https://www.baidu.com
 */
inline fun IjkMediaPlayer.setClearDnsCache(clear: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", clear.ijkBoolean)
}

/**
 * 首选协议：即设置了大部分常见的协议，目前找到的设置较为全面的协议
 * 资料来源：（第431行） https://github.com/bilibili/ijkplayer/blob/e99d640e5fe94c65132379307f92d7180bcde8e7/android/ijkplayer/ijkplayer-java/src/main/java/tv/danmaku/ijk/media/player/IjkMediaPlayer.java
 * @note 该协议使用ijkhttphook之类的协议，可以实现断网重连。 参考（第19条）https://www.jianshu.com/p/220b00d00deb
 */
inline fun IjkMediaPlayer.setPreferredProtocol() {
    setProtocolWhiteList("async,cache,crypto,file,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data")
}

/**
 * 首选协议：支持（设置本地m3u8）
 * 资料来源：（第18条） https://www.jianshu.com/p/220b00d00deb
 */
inline fun IjkMediaPlayer.setPreferredProtocol2() {
    setProtocolWhiteList("crypto,file,http,https,tcp,tls,udp")
}

/**
 * 设置协议白名单：即支持哪些类型的地址
 */
inline fun IjkMediaPlayer.setProtocolWhiteList(value: String) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", value)
}


/**
 * 设置码率
 * todo 只支持硬解，而且要借助MediaCodec对象，留在这里做个笔记
 * @param bps 码率，单位kb
 */
fun IjkMediaPlayer.setBitrate(bps: Int) {
    val bitrate: Bundle = Bundle()
    bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024)
    //todo 将参数设置给解码器
//    mMediaCodec.setParameters(bitrate)
}

/**
 * ffmpeg在avformat_find_stream_info中会读取一部分源文件的音视频数据，来分析文件信息，那么这个操作读取多少数据呢?
 * 答案是： 通过probesize和analyzeduration两个参数来控制。
 * 参考来源：https://www.jianshu.com/p/37d705aa0e01
 *
 * 查询的资料：（来源：https://cloud.tencent.com/developer/article/1357997）
 * 当probesize和analyzeduration过小时，可能会造成预读数据不足，无法解析出码流信息，从而导致播放失败、无音频或无视频的情况。
 * 所以，在服务端对视频格式进行标准化转码，从而确定视频格式，进而再去推算 avformat_find_stream_info 分析码流信息所兼容的最小的 probesize 和 analyzeduration，
 * 就能在保证播放成功率的情况下最大限度地区优化首屏秒开
 */
fun setProbeSizeAndAnalyzeDuration(probeSize: Long, analyzeDuration: Long) {

}

fun IjkMediaPlayer.preferredLiveOptions() {

//    ijkplayer和ffplay在打开rtmp串流视频时，大多数都会遇到5~10秒的延迟，在ffplay播放时，如果加上-fflags nobuffer可以缩短播放的rtmp视频延迟在1s内，而在IjkMediaPlayer中加入
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240L);


//    4:设置播放前的探测时间 1,达到首屏秒开效果
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1);
//    5:播放前的探测Size，默认是1M, 改小一点会出画面更快
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"probesize",1024*10);
//    6:每处理一个packet之后刷新io上下文
//    7: 是否开启预缓冲，一般直播项目会开启，达到秒开的效果，不过带来了播放丢帧卡顿的体验
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"packet-buffering",isBufferCache?1:0);
//    8:播放重连次数
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",5);
//    9:最大缓冲大小,单位kb
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-buffer-size",maxCacheSize);
//    10:跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);
//    11:最大fps
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30);
//    12:设置硬解码方式
//    jkPlayer支持硬解码和软解码。 软解码时不会旋转视频角度这时需要你通过onInfo的what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED去获取角度，自己旋转画面。或者开启硬解硬解码，不过硬解码容易造成黑屏无声（硬件兼容问题），下面是设置硬解码相关的代码
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
//    13.SeekTo设置优化
//    某些视频在SeekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题，通俗一点就是FFMPEG不兼容，视频压缩过于厉害，seek只支持关键帧，出现这个情况就是原始的视频文件中i 帧比较少
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

//    14. 解决m3u8文件拖动问题 比如:一个3个多少小时的音频文件，开始播放几秒中，然后拖动到2小时左右的时间，要loading 10分钟
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek")//设置seekTo能够快速seek到指定位置并播放
//    重要记录，问题列表
//    1. 设置之后，高码率m3u8的播放卡顿，声音画面不同步，或者只有画面，没有声音，或者声音画面不同步
////某些视频在SeekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题，通俗一点就是FFMPEG不兼容，视频压缩过于厉害，seek只支持关键帧，出现这个情况就是原始的视频文件中i 帧比较少
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
////播放前的探测Size，默认是1M, 改小一点会出画面更快
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024 * 10);
}

fun IjkMediaPlayer.options() {
    /*设置解码模式：[1 - 硬解] [0 - 软解]*/
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg2", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg4", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
}

/**
 * 是否在Prepared回调之后，自动开始播放，如果设置为false，则需要在onPrepared回调之后手动调用Start方法才能开始播放
 * 注意：在调用[IjkMediaPlayer.prepareAsync]方法设置之前有效，而且据说播放器每次reset之后，设置的option就无效了
 */
inline fun IjkMediaPlayer.setAutoPlayOnPrepared(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", if (enable) 1 else 0)
}

/**
 * 设置最大缓冲区大小，默认200KB
 * max buffer size should be pre-read
 */
inline fun IjkMediaPlayer.setMaxBufferSize(size: Long = 200 * 1024) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", size)
}

/**
 * 设置最大帧
 */
inline fun IjkMediaPlayer.setMaxFps(fps: Long) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", fps)
}

/**
 * 是否禁用音频
 */
inline fun IjkMediaPlayer.setDisableAudio(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "an", enable.ijkBoolean)
}

/**
 * 是否禁用视频
 */
inline fun IjkMediaPlayer.setDisableVideo(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "vn", enable.ijkBoolean)
}

/**
 * 设置禁用图形显示
 */
inline fun IjkMediaPlayer.setDisableGraphicalDisplay(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "nodisp", enable.ijkBoolean)
}

/**
 * 设置音量；最小值=0，最大值=100
 */
inline fun IjkMediaPlayer.setVolume(volume: Long) {
    require(volume in 0..100) {
        "volume 0=min 100=max"
    }
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "volume", volume)
}


/**
 * 快速模式：不符合规范的优化
 */
inline fun IjkMediaPlayer.setFastModeEnable(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", enable.ijkBoolean)
}

/**
 * 设置循环播放次数
 */
inline fun IjkMediaPlayer.setLoopCount(count: Int) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "loop", count.toLong())
}

/*以下为android仅有的配置*/
inline fun IjkMediaPlayer.setEnableH264(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", enable.ijkBoolean)
}

/**
 * 据不确定资料表示，该配置相当于打开h265硬解
 */
inline fun IjkMediaPlayer.setEnableHEVC(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", enable.ijkBoolean)
}

inline fun IjkMediaPlayer.setEnableMPEG2(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg2", enable.ijkBoolean)
}

inline fun IjkMediaPlayer.setEnableMPEG4(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", enable.ijkBoolean)
}

/**
 * 设置是否启用重连
 */
inline fun IjkMediaPlayer.setEnableReconnect(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", enable.ijkBoolean)
}

//    1: 设置是否开启变调
//


/**
 * MediaCodec: handle resolution change automatically
 */
fun IjkMediaPlayer.setMediacodecHandleResolutionChange(enable: Boolean) {
    setOption(
        IjkMediaPlayer.OPT_CATEGORY_PLAYER,
        "mediacodec-handle-resolution-change",
        enable.ijkBoolean
    )
}


fun IjkMediaPlayer.applyPreferredOptions() {
    applySoftDecodingPreferredOptions()
//    //todo 待确定 : 目前感觉无效
//    //  关闭播放器缓冲，这个必须关闭，否则会出现播放一段时间后，一直卡主，控制台打印 FFP_MSG_BUFFERING_START
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)

//    //硬解码：1、打开，0、关闭
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
//
//    //ijkplayer和ffplay在打开rtmp串流视频时，大多数都会遇到5~10秒的延迟，在ffplay播放时，如果加上-fflags nobuffer可以缩短播放的rtmp视频延迟在1s内，而在IjkMediaPlayer中加入
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)

//    //解决m3u8文件拖动问题 比如:一个3个多少小时的音频文件，开始播放几秒中，然后拖动到2小时左右的时间，要loading 10分钟
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek")//设置seekTo能够快速seek到指定位置并播放


//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
}

@PublishedApi
internal inline val Boolean.ijkBoolean: Long
    get() = if (this) 1 else 0