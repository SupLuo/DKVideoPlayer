@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.internal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import java.util.concurrent.Executors

//避免反复读取
internal val sdkInt: Int = Build.VERSION.SDK_INT

/**
 * 从上下文中获取[Activity]
 */
fun Context.getActivityContext(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

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


internal const val NO_NETWORK = 0
internal const val NETWORK_CLOSED = 1
internal const val NETWORK_ETHERNET = 2
internal const val NETWORK_WIFI = 3
internal const val NETWORK_MOBILE = 4
internal const val NETWORK_UNKNOWN = -1

/**
 * 判断当前网络类型
 */
internal fun Context.getNetworkType(): Int {
    val connectMgr =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NO_NETWORK
    // 没有任何网络
    val networkInfo = connectMgr.activeNetworkInfo ?: return NO_NETWORK

    if (!networkInfo.isConnected) {
        // 网络断开或关闭
        return NETWORK_CLOSED
    }
    if (networkInfo.type == ConnectivityManager.TYPE_ETHERNET) {
        // 以太网网络
        return NETWORK_ETHERNET
    } else if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
        // wifi网络，当激活时，默认情况下，所有的数据流量将使用此连接
        return NETWORK_WIFI
    } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
        // 移动数据连接,不能与连接共存,如果wifi打开，则自动关闭
        when (networkInfo.subtype) {
            // 2G
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN,
                // 3G
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP,
                // 4G
            TelephonyManager.NETWORK_TYPE_LTE,
                // 5G
            TelephonyManager.NETWORK_TYPE_NR
            -> return NETWORK_MOBILE
        }
    }
    // 未知网络
    return NETWORK_UNKNOWN
}

