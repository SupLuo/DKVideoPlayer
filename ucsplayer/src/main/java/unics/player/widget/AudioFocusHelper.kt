package unics.player.widget

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import unics.player.UCSPManager
import unics.player.kernel.UCSPlayer
import java.lang.ref.WeakReference

/**
 * 音频焦点 帮助类
 * @see .requestFocus
 * @see .abandonFocus
 */
class AudioFocusHelper(context: Context) {

    private var mHandler: Handler? = null

    private var mPlayerRef: WeakReference<UCSPlayer>? = null
    private val mAudioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    private var mStartRequested = false
    private var mPausedForLoss = false
    private var mCurrentFocus = if (Build.VERSION.SDK_INT >= 26) AudioManager.AUDIOFOCUS_NONE else 0

    /**
     * 是否启用
     */
    var isEnable: Boolean = UCSPManager.isAudioFocusEnabled

    /**
     * 播放器是否静音：如果播放器设置静音，那么音频焦点变化则不会改变播放器的音量
     */
    var isPlayerMute:Boolean = false

    /**
     * 不排除在子线程中回调：https://blog.csdn.net/baidu_27419681/article/details/113751458
     * 因此这里最好是规避一下是否是主线程
     */
    private val mOnAudioFocusChange =
        OnAudioFocusChangeListener { focusChange ->
            if (mCurrentFocus == focusChange) {
                return@OnAudioFocusChangeListener
            }
            //这里应该先改变状态，然后在post，否则在极短时间内存在理论上的多次post
            mCurrentFocus = focusChange

            if (Looper.myLooper() == Looper.getMainLooper()) {
                handleAudioFocusChange(focusChange)
            } else {
                synchronized(this) {
                    if (mHandler == null) {
                        mHandler = Handler(Looper.getMainLooper())
                    }
                    //由于onAudioFocusChange有可能在子线程调用，故通过此方式切换到主线程去执行
                    mHandler!!.post {
                        handleAudioFocusChange(focusChange)
                    }
                }
            }
        }

    private fun handleAudioFocusChange(focusChange: Int) {
        try { //进行异常捕获，避免因为音频焦点导致crash
            val player = mPlayerRef?.get() ?: return
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                    if (mStartRequested || mPausedForLoss) {
                        player.start()
                        mStartRequested = false
                        mPausedForLoss = false
                    }
                    if (!isPlayerMute) //恢复音量
                        player.setVolume(1.0f, 1.0f)
                }
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (player.isPlaying()) {
                    mPausedForLoss = true
                    player.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (player.isPlaying() && !isPlayerMute) {
                    player.setVolume(0.1f, 0.1f)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun bindPlayer(player: UCSPlayer) {
        mPlayerRef = WeakReference(player)
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

}