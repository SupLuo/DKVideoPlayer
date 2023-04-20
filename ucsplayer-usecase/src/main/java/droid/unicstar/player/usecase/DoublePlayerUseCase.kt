package droid.unicstar.player.usecase

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import droid.unicstar.player.UCSPlayerProxy
import droid.unicstar.player.controller.UCSContainerControl

/**
 * 双播放器场景：两个播放器共享同一个视图
 * 即两个播放器公用同一个图层
 * @param mOne 第一个播放器
 * @param mTwo 第二个播放器
 * @see playOne 使用第一个播放器进行播放
 * @see playTwo 使用第二个播放器进行播放
 */
class DoublePlayerUseCase(
    private val mOne: UCSPlayerProxy,
    private val mTwo: UCSPlayerProxy,
    private val mContainer: UCSContainerControl
) : LifecycleObserver {

    private var mCurrent: UCSPlayerProxy? = null

    val currentPlayer: UCSPlayerProxy? get() = mCurrent

    /**
     * 绑定生命周期，自动管理
     */
    fun bindLifecycleOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    /**
     * 使用第一个进行播放
     */
    fun playOne(seekMsec: Long = 0) {
        if (mCurrent == mOne)
            return
        unfocus(mTwo)
        focus(mOne, seekMsec)
    }

    /**
     * 使用第二个进行播放
     */
    fun playTwo(seekMsec: Long = 0) {
        if (mCurrent == mTwo)
            return
        unfocus(mOne)
        focus(mTwo, seekMsec)
    }

    private fun unfocus(player: UCSPlayerProxy) {
        if (player.isPlaying()) {
            player.pause()
            player.setDisplay(null)
            player.setSurface(null)
        }
    }

    private fun focus(player: UCSPlayerProxy, seekMsec: Long = 0) {
        mContainer.bindPlayer(player)
        mCurrent = player
        if (!player.isPlaying()) {
            player.resume()
        }

        if (seekMsec > 0) {
            player.seekTo(seekMsec)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        mCurrent?.resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        mCurrent?.pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        mOne.release()
        mTwo.release()
        mContainer.release()
    }
}