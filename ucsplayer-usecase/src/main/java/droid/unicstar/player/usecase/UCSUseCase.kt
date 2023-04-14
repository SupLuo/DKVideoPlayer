package droid.unicstar.player.usecase

import droid.unicstar.videoplayer.UNSPlayerProxy
import droid.unicstar.videoplayer.controller.UNSContainerControl

object UCSUseCase {

    /**
     * 双播放器场景：两个播放器共享同一个视图
     */
    @JvmStatic
    fun newDoublePlayerScene(
        one: UNSPlayerProxy,
        two: UNSPlayerProxy,
        container: UNSContainerControl
    ): DoublePlayerUseCase {
        return DoublePlayerUseCase(one, two, container)
    }

}