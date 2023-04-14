package droid.unicstar.player.usecase

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import droid.unicstar.videoplayer.UNSPlayerProxy
import droid.unicstar.videoplayer.controller.UNSContainerControl

/**
 * 双播放器场景：两个播放器共享同一个视图
 */
class DoublePlayerUseCase(
    private val mOne: UNSPlayerProxy,
    private val mTwo: UNSPlayerProxy,
    private val mContainer: UNSContainerControl
) : LifecycleObserver {

    private var mCurrent: UNSPlayerProxy? = null

    val currentPlayer: UNSPlayerProxy? get() = mCurrent

    fun bindLifecycleOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    /**
     * 使用第一个进行播放
     */
    fun playOne() {
        if (mCurrent == mOne)
            return
        unfocus(mTwo)
        focus(mOne)
    }

    /**
     * 使用第二个进行播放
     */
    fun playTwo() {
        if (mCurrent == mTwo)
            return
        unfocus(mOne)
        focus(mTwo)
    }

    private fun unfocus(player: UNSPlayerProxy) {
        if (player.isPlaying()) {
            player.pause()
            player.setDisplay(null)
            player.setSurface(null)
        }
    }

    private fun focus(player: UNSPlayerProxy) {
        if (!player.isPlaying()) {
            player.resume()
        }
        mContainer.bindPlayer(player)
        mCurrent = player
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