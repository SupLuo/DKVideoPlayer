package xyz.doikki.videoplayer.ijk

import tv.danmaku.ijk.media.player.IjkMediaPlayer


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