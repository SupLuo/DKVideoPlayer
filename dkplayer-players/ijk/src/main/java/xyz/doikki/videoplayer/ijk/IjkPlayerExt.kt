package xyz.doikki.videoplayer.ijk

import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * 优化和解决方案文档参考：https://www.cnblogs.com/marklove/articles/10608812.html
 *
 */

fun IjkMediaPlayer.preferredLiveOptions(){

//    ijkplayer和ffplay在打开rtmp串流视频时，大多数都会遇到5~10秒的延迟，在ffplay播放时，如果加上-fflags nobuffer可以缩短播放的rtmp视频延迟在1s内，而在IjkMediaPlayer中加入
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0L);
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1L);

//    1: 设置是否开启变调
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"soundtouch",isModifyTone?0:1);
//    2:设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
//    setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC,"skip_loop_filter",isSkipLoopFilter?0:48L);
//    3:设置播放前的最大探测时间
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"analyzemaxduration",100L);
//    4:设置播放前的探测时间 1,达到首屏秒开效果
    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"analyzeduration",1);
//    5:播放前的探测Size，默认是1M, 改小一点会出画面更快
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"probesize",1024*10);
//    6:每处理一个packet之后刷新io上下文
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"flush_packets",1L);
//    7: 是否开启预缓冲，一般直播项目会开启，达到秒开的效果，不过带来了播放丢帧卡顿的体验
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"packet-buffering",isBufferCache?1:0);
//    8:播放重连次数
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"reconnect",5);
//    9:最大缓冲大小,单位kb
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-buffer-size",maxCacheSize);
//    10:跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"framedrop",5);
//    11:最大fps
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-fps",30);
//    12:设置硬解码方式
//    jkPlayer支持硬解码和软解码。 软解码时不会旋转视频角度这时需要你通过onInfo的what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED去获取角度，自己旋转画面。或者开启硬解硬解码，不过硬解码容易造成黑屏无声（硬件兼容问题），下面是设置硬解码相关的代码
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
//    13.SeekTo设置优化
//    某些视频在SeekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题，通俗一点就是FFMPEG不兼容，视频压缩过于厉害，seek只支持关键帧，出现这个情况就是原始的视频文件中i 帧比较少
//
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
fun IjkMediaPlayer.setAutoPlayOnPrepared(enable: Boolean) {
    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", if (enable) 1 else 0)
}

fun IjkMediaPlayer.applyPreferredOptions(){
//    //todo 待确定 : 目前感觉无效
//    //  关闭播放器缓冲，这个必须关闭，否则会出现播放一段时间后，一直卡主，控制台打印 FFP_MSG_BUFFERING_START
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)
//
//    //硬解码：1、打开，0、关闭
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
////    //软解码：1、打开，0、关闭
////    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "videotoolbox", 1)
//
//    //ijkplayer和ffplay在打开rtmp串流视频时，大多数都会遇到5~10秒的延迟，在ffplay播放时，如果加上-fflags nobuffer可以缩短播放的rtmp视频延迟在1s内，而在IjkMediaPlayer中加入
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", 100)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 10240)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
//
//
//    //设置播放前的探测时间 1,达到首屏秒开效果
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,"analyzeduration",1)
//
//    //跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"framedrop",5)
//
//    //最大fps
//    setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"max-fps",30)
//
//    //解决m3u8文件拖动问题 比如:一个3个多少小时的音频文件，开始播放几秒中，然后拖动到2小时左右的时间，要loading 10分钟
//    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek")//设置seekTo能够快速seek到指定位置并播放

    setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
}