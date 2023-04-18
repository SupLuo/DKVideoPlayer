package droid.unicstar.player

import androidx.annotation.IntDef

/**
 * 屏幕模式
 */
@IntDef(
    UCSVideoView.SCREEN_MODE_NORMAL,
    UCSVideoView.SCREEN_MODE_FULL,
    UCSVideoView.SCREEN_MODE_TINY
)
@Retention(AnnotationRetention.SOURCE)
annotation class ScreenMode