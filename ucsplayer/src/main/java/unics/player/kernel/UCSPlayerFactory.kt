package unics.player.kernel

import android.content.Context
import unics.player.kernel.sys.SysPlayer
import unics.player.kernel.sys.SysPlayerFactory

/**
 * 此接口使用方法：
 * 1.继承[UCSPlayer]扩展自己的播放器。
 * 2.继承此接口并实现[create]，返回步骤1中的播放器。
 * 3a.全局切换：通过[unics.player.UCSPManager.playerFactory] 设置步骤2的实例
 * 3b.临时切换：通过[unics.player.controller.UCSPlayerControl.setPlayerFactory]（该方式有两种场景，一种是使用UCSVideoView，一种是使用PlayerProxy） 设置步骤2的实例
 *
 * 步骤1和2 可参照[unics.player.kernel.sys.SysPlayer]和[unics.player.kernel.sys.SysPlayerFactory]的实现。
 */
fun interface UCSPlayerFactory<P : UCSPlayer> {

    /**
     * @param context 注意内存泄露：内部尽可能使用context.getApplicationContext();
     * 绝大部分情况下，player的创建通过ApplicationContext创建不会有问题
     * @return
     */
    fun create(context: Context): P

    companion object {

        /**
         * 创建[SysPlayer]的工厂类，不推荐，系统的MediaPlayer兼容性较差，建议使用IjkPlayer或者ExoPlayer
         */
        @Deprecated("兼容性较差：比如某些盒子上不能配合texture使用")
        @JvmStatic
        fun system(): UCSPlayerFactory<SysPlayer> {
            return SysPlayerFactory()
        }
    }

}