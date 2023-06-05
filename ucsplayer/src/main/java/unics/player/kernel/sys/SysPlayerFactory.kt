package unics.player.kernel.sys

import android.content.Context
import unics.player.kernel.UCSPlayerFactory

/**
 * 创建[SysPlayer]的工厂类，不推荐，系统的MediaPlayer兼容性较差，建议使用IjkPlayer或者ExoPlayer
 * @note 本身可以采用lambda实现，但是不利于调试时通过classname获取Player的名字
 */
class SysPlayerFactory : UCSPlayerFactory<SysPlayer> {

    override fun create(context: Context): SysPlayer {
        return SysPlayer()
    }

}