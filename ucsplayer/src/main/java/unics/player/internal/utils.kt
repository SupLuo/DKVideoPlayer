@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.internal

import android.content.Context
import android.graphics.Point
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import android.view.*
import android.widget.Toast
import androidx.annotation.StringRes
import java.util.concurrent.Executors
import kotlin.math.abs


internal const val INVALIDATE_SEEK_POSITION = -1

/**
 * 判断两个浮点数是否约等于
 */
internal inline fun Float.approximatelyEquals(other: Float, tolerance: Double = 1e-10): Boolean {
    return abs(this - other) < tolerance
}

//避免反复读取
internal val sdkInt: Int = Build.VERSION.SDK_INT

internal val threadPool = Executors.newCachedThreadPool()

/**
 * 释放播放器：为避免release导致anr，先执行stop，再执行release，并且可以考虑把release放在子线程执行
 */
internal fun releasePlayer(mediaPlayer: MediaPlayer) {
    mediaPlayer.apply {
        //必须在播放过程中才可以调用stop，否则可能出现“stop called in state 1, mPlayer(0x0)”问题
        if(mediaPlayer.isPlaying)
            stop()
        mediaPlayer.reset()
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
                plogi { "releasePlayer($temp) -> invoke on thread" }
                temp.release()
            } catch (e: Exception) {
                ploge(e) {
                    "release($temp) on thread error"
                }
            }
        }
    })
}

/**
 * 从Parent中移除自己
 */
internal inline fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

/**
 * 能否获取焦点
 */
internal val View.canTakeFocus: Boolean
    get() = isFocusable && this.visibility == View.VISIBLE && isEnabled

@PublishedApi
internal inline fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

@PublishedApi
internal inline fun Context.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageId, length).show()
}

@PublishedApi
internal inline fun View.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    context.toast(message, length)
}

@PublishedApi
internal inline fun View.toast(@StringRes messageId: Int, length: Int = Toast.LENGTH_SHORT) {
    context.toast(messageId, length)
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


/**
 * 获取状态栏高度
 */
fun getStatusBarHeight(context: Context): Double {
    return try {
        var statusBarHeight = 0
        //获取status_bar_height资源的ID
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        statusBarHeight.toDouble()
    } catch (e: Throwable) {
        e.printStackTrace()
        0.0
    }
}

/**
 * 获取竖屏下状态栏高度
 */
fun getStatusBarHeightPortrait(context: Context): Double {
    return try {
        var statusBarHeight = 0
        //获取status_bar_height_portrait资源的ID
        val resourceId =
            context.resources.getIdentifier("status_bar_height_portrait", "dimen", "android")
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        statusBarHeight.toDouble()
    } catch (e: Throwable) {
        e.printStackTrace()
        0.0
    }
}

/**
 * 获取NavigationBar的高度
 */
fun getNavigationBarHeight(context: Context): Int {
    if (!hasNavigationBar(context)) {
        return 0
    }
    val resources = context.resources
    val resourceId = resources.getIdentifier(
        "navigation_bar_height",
        "dimen", "android"
    )
    //获取NavigationBar的高度
    return resources.getDimensionPixelSize(resourceId)
}

/**
 * 是否存在NavigationBar
 */
fun hasNavigationBar(context: Context): Boolean {
    return if (sdkInt >= 17) {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return true
        val display = windowManager.defaultDisplay
        val size = Point()
        val realSize = Point()
        display.getSize(size)
        display.getRealSize(realSize)
        realSize.x != size.x || realSize.y != size.y
    } else {
        val menu = ViewConfiguration.get(context).hasPermanentMenuKey()
        val back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        !(menu || back)
    }
}

/**
 * 获取屏幕宽度
 */
fun getScreenWidth(context: Context, isIncludeNav: Boolean): Int {
    return if (isIncludeNav) {
        context.resources.displayMetrics.widthPixels + getNavigationBarHeight(context)
    } else {
        context.resources.displayMetrics.widthPixels
    }
}

/**
 * 获取屏幕高度
 */
fun getScreenHeight(context: Context, isIncludeNav: Boolean): Int {
    return if (isIncludeNav) {
        context.resources.displayMetrics.heightPixels + getNavigationBarHeight(context)
    } else {
        context.resources.displayMetrics.heightPixels
    }
}