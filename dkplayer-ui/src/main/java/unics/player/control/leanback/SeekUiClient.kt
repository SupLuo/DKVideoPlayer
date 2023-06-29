package unics.player.control.leanback

abstract class SeekUiClient {

    open fun isSeekEnabled(): Boolean {
        return false
    }

    open fun onSeekStarted() {}

//    open fun getPlaybackSeekDataProvider(): PlaybackSeekDataProvider? {
//        return null
//    }

    open fun onSeekPositionChanged(pos: Long) {}

    open fun onSeekFinished(cancelled: Boolean) {}
}