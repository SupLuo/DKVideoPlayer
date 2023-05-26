@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.internal

import android.media.MediaPlayer
import android.os.Build
import unicstar.oknote.OkNote
import java.util.concurrent.Executors

@PublishedApi
internal const val TAG = "UCSPlayer"

internal val threadPool = Executors.newCachedThreadPool()

/**
 * 释放播放器：为避免release导致anr，先执行stop，再执行release，并且可以考虑把release放在子线程执行
 */
internal fun releasePlayer(mediaPlayer: MediaPlayer) {
    mediaPlayer.apply {
        stop()
        setOnErrorListener(null)
        setOnCompletionListener(null)
        setOnInfoListener(null)
        setOnBufferingUpdateListener(null)
        setOnPreparedListener(null)
        setOnVideoSizeChangedListener(null)
        setDisplay(null)
        setSurface(null)
    }
    threadPool.execute(object : Runnable {
        val temp = mediaPlayer
        override fun run() {
            try {
                plogi() { "releasePlayer($temp) -> invoke on thread" }
                temp.release()
            } catch (e: Exception) {
                ploge(e) {
                    "release($temp) on thread error"
                }
            }
        }
    })
}

//避免反复读取
internal val sdkInt: Int = Build.VERSION.SDK_INT

inline fun plogv(message: String) {
    OkNote.v(TAG, message)
}

inline fun plogd(message: String) {
    OkNote.d(TAG, message)
}

inline fun plogi(message: String) {
    OkNote.i(TAG, message)
}

inline fun plogw(message: String) {
    OkNote.w(TAG, message)
}

inline fun plogw(e: Throwable, message: String) {
    OkNote.w(TAG, message, e)
}

inline fun ploge(message: String) {
    OkNote.e(TAG, message)
}

inline fun ploge(e: Throwable, message: String) {
    OkNote.e(TAG, message, e)
}

inline fun plogv(creator: () -> String) {
    OkNote.logv(TAG, creator)
}

inline fun plogd(creator: () -> String) {
    OkNote.logd(TAG, creator)
}

inline fun plogi(creator: () -> String) {
    OkNote.logi(TAG, creator)
}

inline fun plogw(creator: () -> String) {
    OkNote.logw(TAG, creator)
}

inline fun plogw(e: Throwable, creator: () -> String) {
    OkNote.logw(TAG, e, creator)
}

inline fun ploge(creator: () -> String) {
    OkNote.loge(TAG, creator)
}

inline fun ploge(e: Throwable, creator: () -> String) {
    OkNote.loge(TAG, e, creator)
}

inline fun plogv2(subTag: String, creator: () -> String) {
    OkNote.logv(TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogd2(subTag: String, creator: () -> String) {
    OkNote.logd(TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogi2(subTag: String, creator: () -> String) {
    OkNote.logi(TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogw2(subTag: String, creator: () -> String) {
    OkNote.logw(TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogw2(subTag: String, e: Throwable, creator: () -> String) {
    OkNote.logw(TAG, e) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun ploge2(subTag: String, creator: () -> String) {
    OkNote.loge(TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun ploge2(subTag: String, e: Throwable, creator: () -> String) {
    OkNote.loge(TAG, e) {
        "$subTag :${creator.invoke()}"
    }
}
