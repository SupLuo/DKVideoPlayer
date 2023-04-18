package droid.unicstar.player.player


abstract class BaseUCSPlayer : UCSPlayer {

    /**
     * 播放器事件回调
     */
    protected var mEventListener: UCSPlayer.EventListener? = null

    override fun setEventListener(eventListener: UCSPlayer.EventListener?) {
        mEventListener = eventListener
    }


}