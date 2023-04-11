package droid.unicstar.videoplayer.player


abstract class AbstractCSPlayer : UNSPlayer {

    /**
     * 播放器事件回调
     */
    protected var eventListener: UNSPlayer.EventListener? = null
        private set

    override fun setEventListener(eventListener: UNSPlayer.EventListener?) {
        this.eventListener = eventListener
    }

}