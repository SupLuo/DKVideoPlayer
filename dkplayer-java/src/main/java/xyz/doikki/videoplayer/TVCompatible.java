package xyz.doikki.videoplayer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 表示已兼容TV
 */
@Retention(RetentionPolicy.SOURCE)
public @interface TVCompatible {
    String message() default "";
}
