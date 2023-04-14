package droid.unicstar.videoplayer.player.sys

import android.content.Context
import droid.unicstar.videoplayer.player.UNSPlayerFactory

/**
 * 创建[SysUNSPlayer]的工厂类，不推荐，系统的MediaPlayer兼容性较差，建议使用IjkPlayer或者ExoPlayer
 * todo 本身可以采用lambda实现，但是不利于调试时通过classname获取Player的名字
 */
class SysUNSPlayerFactory : UNSPlayerFactory<SysUNSPlayer> {

    override fun create(context: Context): SysUNSPlayer {
        return SysUNSPlayer()
    }

}