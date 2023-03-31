package droid.unicstar.videoplayer.player


abstract class AbstractCSPlayer : CSPlayer {

    /**
     * 播放器事件回调
     */
    protected var eventListener: CSPlayer.EventListener? = null
        private set

    override fun setEventListener(eventListener: CSPlayer.EventListener?) {
        this.eventListener = eventListener
    }

}