package unics.player;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 屏幕模式
 */
@IntDef({ScreenMode.UNKNOWN,
        ScreenMode.NORMAL,
        ScreenMode.FULL_SCREEN,
        ScreenMode.TINY_SCREEN})
@Retention(RetentionPolicy.SOURCE)
public @interface ScreenMode {

    /**
     * 未知
     */
    int UNKNOWN = 0;

    /**
     * 普通模式
     */
    int NORMAL = 10;

    /**
     * 全屏模式
     */
    int FULL_SCREEN = 11;

    /**
     * 小窗模式
     */
    int TINY_SCREEN = 22;

}
