package droid.unicstar.player.usecase

import droid.unicstar.player.UCSPlayerProxy
import droid.unicstar.player.controller.UCSContainerControl

object UCSUseCase {

    /**
     * 双播放器场景：两个播放器共享同一个视图
     */
    @JvmStatic
    fun newDoublePlayerScene(
        one: UCSPlayerProxy,
        two: UCSPlayerProxy,
        container: UCSContainerControl
    ): DoublePlayerUseCase {
        return DoublePlayerUseCase(one, two, container)
    }

}