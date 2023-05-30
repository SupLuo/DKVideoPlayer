package unics.player.kernel

abstract class BasePlayer : UCSPlayer {

    /**
     * 播放器事件回调
     */
    protected var mEventListener: UCSPlayer.EventListener? = null

    override fun setEventListener(eventListener: UCSPlayer.EventListener?) {
        mEventListener = eventListener
    }

    protected fun newPlayerException(what: Int, extra: Int): Throwable {
        return PlayerException.create(what, extra)
    }
}