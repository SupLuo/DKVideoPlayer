package xyz.doikki.videoplayer

import android.content.res.AssetFileDescriptor

interface PlayerCore {
    /**
     * 设置播放地址
     *
     * @param path 播放地址
     */
    fun setDataSource(path: String) {
        setDataSource(path, null)
    }

    /**
     * 设置播放地址
     *
     * @param path    播放地址
     * @param headers 播放地址请求头
     */
    fun setDataSource(path: String, headers: Map<String, String>?)

    /**
     * 用于播放raw和asset里面的视频文件
     */
    fun setDataSource(fd: AssetFileDescriptor)
}