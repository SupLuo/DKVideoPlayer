package droid.unicstar.player.player.sys

import android.content.Context
import droid.unicstar.player.player.UCSPlayerFactory

/**
 * 创建[SysUCSPlayer]的工厂类，不推荐，系统的MediaPlayer兼容性较差，建议使用IjkPlayer或者ExoPlayer
 * todo 本身可以采用lambda实现，但是不利于调试时通过classname获取Player的名字
 */
class SysUCSPlayerFactory : UCSPlayerFactory<SysUCSPlayer> {

    override fun create(context: Context): SysUCSPlayer {
        return SysUCSPlayer()
    }

}