@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager

/**
 * 适配刘海屏，针对Android P以上系统
 *
 * @param isAdapt 是否适配刘海屏，false则会使用默认方式
 */
@SuppressLint("NewApi")
fun adaptCutout(context: Context, isAdapt: Boolean) {
    val activity = UCSPUtil.getActivityContext(context) ?: return
    if (sdkInt < 28) return
    val lp = activity.window.attributes
    if (isAdapt) {
        if (lp.layoutInDisplayCutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
            return
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    } else {
        if (lp.layoutInDisplayCutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT)
            return
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
    }
    activity.window.attributes = lp
}

/**
 * 是否为允许全屏界面显示内容到刘海区域的刘海屏机型（与AndroidManifest中配置对应）
 */
inline fun allowDisplayToCutout(activity: Activity): Boolean {
    return allowDisplayToCutout(activity.window)
}

/**
 * 是否为允许全屏界面显示内容到刘海区域的刘海屏机型（与AndroidManifest中配置对应）
 */
@SuppressLint("NewApi")
fun allowDisplayToCutout(window: Window): Boolean {
    return if (sdkInt >= 28) {
        // 9.0系统全屏界面默认会保留黑边，不允许显示内容到刘海区域
        val windowInsets = window.decorView.rootWindowInsets ?: return false
        val displayCutout = windowInsets.displayCutout ?: return false
        val boundingRects = displayCutout.boundingRects
        boundingRects.size > 0
    } else {
        val context = window.context
        (hasCutoutHuawei(context)
                || hasCutoutOPPO(context)
                || hasCutoutVIVO(context)
                || hasCutoutXIAOMI(context))
    }
}

/**
 * 是否是华为刘海屏机型
 */
private fun hasCutoutHuawei(context: Context): Boolean {
    return if (!Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
        false
    } else try {
        val cl = context.classLoader
        val hwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
        if (hwNotchSizeUtil != null) {
            val get = hwNotchSizeUtil.getMethod("hasNotchInScreen")
            return get.invoke(hwNotchSizeUtil) as Boolean
        }
        false
    } catch (e: Throwable) {
        false
    }
}

/**
 * 是否是小米刘海屏机型
 */
@SuppressLint("PrivateApi")
private fun hasCutoutXIAOMI(context: Context): Boolean {
    return if (!Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
        false
    } else try {
        val cl = context.classLoader
        val systemProperties = cl.loadClass("android.os.SystemProperties")
        val paramTypes = arrayOf(String::class.java, Int::class.javaPrimitiveType)
        val getInt = systemProperties.getMethod("getInt", *paramTypes)
        //参数
        val params = arrayOf("ro.miui.notch", 0)
        getInt.invoke(systemProperties, *params) == 1
    } catch (e: Throwable) {
        false
    }
}


/**
 * 是否是vivo刘海屏机型
 */
@SuppressLint("PrivateApi")
private fun hasCutoutVIVO(context: Context): Boolean {
    return if (!Build.MANUFACTURER.equals("vivo", ignoreCase = true)) {
        false
    } else try {
        val cl = context.classLoader
        val ftFeatureUtil = cl.loadClass("android.util.FtFeature")
        if (ftFeatureUtil != null) {
            val get = ftFeatureUtil.getMethod("isFeatureSupport", Int::class.javaPrimitiveType)
            return get.invoke(ftFeatureUtil, 0x00000020) as Boolean
        }
        false
    } catch (e: Throwable) {
        false
    }
}

/**
 * 是否是oppo刘海屏机型
 */
private fun hasCutoutOPPO(context: Context): Boolean {
    return if (!Build.MANUFACTURER.equals("oppo", ignoreCase = true)) {
        false
    } else context.packageManager.hasSystemFeature("com.oppo.feature.screen.heteromorphism")
}