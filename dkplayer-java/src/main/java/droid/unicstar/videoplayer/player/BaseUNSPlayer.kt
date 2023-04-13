package droid.unicstar.videoplayer.player


abstract class BaseUNSPlayer : UNSPlayer {

    /**
     * 播放器事件回调
     */
    protected var mEventListener: UNSPlayer.EventListener? = null

    override fun setEventListener(eventListener: UNSPlayer.EventListener?) {
        mEventListener = eventListener
    }


}