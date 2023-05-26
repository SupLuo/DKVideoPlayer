package droid.unicstar.player.usecase

import unics.player.kernel.PlayerProxy
import unics.player.controller.UCSContainerControl

object UCSUseCase {

    /**
     * 双播放器场景：两个播放器共享同一个视图
     */
    @JvmStatic
    fun newDoublePlayerScene(
        one: PlayerProxy,
        two: PlayerProxy,
        container: UCSContainerControl
    ): DoublePlayerUseCase {
        return DoublePlayerUseCase(one, two, container)
    }

}