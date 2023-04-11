package xyz.doikki.videoplayer.internal

import android.media.AudioManager

/**
 * 播放器焦点处理
 */
class PlayerAudioFocusHandler: AudioManager.OnAudioFocusChangeListener {

    override fun onAudioFocusChange(focusChange: Int) {
        try { //进行异常捕获，避免因为音频焦点导致crash
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                    if (mStartRequested || mPausedForLoss) {
                        videoView.start()
                        mStartRequested = false
                        mPausedForLoss = false
                    }
                    if (!videoView.isMute) //恢复音量
                        videoView.setVolume(1.0f, 1.0f)
                }
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (videoView.isPlaying()) {
                    mPausedForLoss = true
                    videoView.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (videoView.isPlaying() && !videoView.isMute) {
                    videoView.setVolume(0.1f, 0.1f)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}