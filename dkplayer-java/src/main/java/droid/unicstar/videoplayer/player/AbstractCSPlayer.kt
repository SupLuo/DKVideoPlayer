package droid.unicstar.videoplayer.player


abstract class AbstractCSPlayer : UNSPlayer {

    /**
     * 播放器事件回调
     */
    protected var mEventListeners: UNSPlayer.EventListener? = null
        private set

    override fun addEventListener(eventListener: UNSPlayer.EventListener?) {
        this.mEventListeners = eventListener
    }

}