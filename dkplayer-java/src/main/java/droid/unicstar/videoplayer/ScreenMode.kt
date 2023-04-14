package droid.unicstar.videoplayer

import androidx.annotation.IntDef

/**
 * 屏幕模式
 */
@IntDef(
    UNSVideoView.SCREEN_MODE_NORMAL,
    UNSVideoView.SCREEN_MODE_FULL,
    UNSVideoView.SCREEN_MODE_TINY
)
@Retention(AnnotationRetention.SOURCE)
annotation class ScreenMode