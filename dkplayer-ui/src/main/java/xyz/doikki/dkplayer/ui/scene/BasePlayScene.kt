package xyz.doikki.dkplayer.ui.scene

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import droid.unicstar.videoplayer.UNSVideoView

/**
 * @see bindLifecycle 绑定生命周期，如果绑定了生命周期，则在onPause的时候，会自动暂停播放，在界面销毁的时候释放播放器
 */
abstract class BasePlayScene() : LifecycleObserver {

    abstract fun getVideoView(): UNSVideoView?

    /**
     * 此方法用于恢复播放
     * 该方法没有绑定生命周期，因为有些时候当界面resume的时候，用户可能并不想播放器恢复播放，而是由用户或者开发者逻辑去控制
     */
    fun onResume() {
        getVideoView()?.resume()
    }

    /**
     * 如果调用[bindLifecycle]绑定了生命周期，则该方法会自动调用，否则需要用户自己调用
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        getVideoView()?.pause()
    }

    /**
     * 如果调用[bindLifecycle]绑定了生命周期，则该方法会自动调用，否则需要用户自己调用
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        getVideoView()?.release()
    }

    /**
     * 绑定生命周期
     */
    fun bindLifecycle(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
        owner.lifecycle.addObserver(this)
    }

}