package xyz.doikki.videoplayer.internal

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Handler
import android.os.Looper
import droid.unicstar.videoplayer.CSVideoView
import java.lang.ref.WeakReference

/**
 * 音频焦点 帮助类
 * @see .requestFocus
 * @see .abandonFocus
 */
class AudioFocusHelper(videoView: CSVideoView) {

    private val mHandler = Handler(Looper.getMainLooper())
    private val mWeakVideoView: WeakReference<CSVideoView>
    private val mAudioManager: AudioManager?
    private var mStartRequested = false
    private var mPausedForLoss = false
    private var mCurrentFocus = 0

    /**
     * 是否启用
     */
    var isEnable: Boolean = true

    private val mOnAudioFocusChange =
        OnAudioFocusChangeListener { focusChange ->
            if (mCurrentFocus == focusChange) {
                return@OnAudioFocusChangeListener
            }
            //这里应该先改变状态，然后在post，否则在极短时间内存在理论上的多次post
            mCurrentFocus = focusChange

            //由于onAudioFocusChange有可能在子线程调用，
            //故通过此方式切换到主线程去执行
            mHandler.post {
                try { //进行异常捕获，避免因为音频焦点导致crash
                    handleAudioFocusChange(focusChange)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

    private fun handleAudioFocusChange(focusChange: Int) {
        val videoView = mWeakVideoView.get() ?: return
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
    }

    /**
     * Requests to obtain the audio focus
     * 请求音频焦点
     */
    fun requestFocus() {
        if (!isEnable || mAudioManager == null) {
            return
        }

        if (mCurrentFocus == AudioManager.AUDIOFOCUS_GAIN) {
            return
        }
        val status = mAudioManager.requestAudioFocus(
            mOnAudioFocusChange,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
            mCurrentFocus = AudioManager.AUDIOFOCUS_GAIN
            return
        }
        mStartRequested = true
    }

    /**
     * Requests the system to drop the audio focus
     * 放弃音频焦点
     */
    fun abandonFocus() {
        if (!isEnable || mAudioManager == null) {
            return
        }
        mStartRequested = false
        mAudioManager.abandonAudioFocus(mOnAudioFocusChange)
    }

    init {
        mWeakVideoView = WeakReference(videoView)
        mAudioManager =
            videoView.context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}