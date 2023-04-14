package xyz.doikki.videoplayer.controller;

import androidx.annotation.IntRange;

import droid.unicstar.videoplayer.controller.UNSVideoViewControl;

/**
 * 视图控制器
 */
public interface VideoViewController {
    
    /**
     * 控制视图是否处于显示状态
     */
    boolean isShowing();

    /**
     * 显示控制视图
     */
    void show();

    /**
     * 隐藏控制视图
     */
    void hide();

    /**
     * 启动自动隐藏控制器视图
     */
    void startFadeOut();

    /**
     * 移除自动隐藏控制器视图
     */
    void stopFadeOut();

    /**
     * 设置自动隐藏倒计时持续的时间
     *
     * @param timeout 默认4000，比如大于0才会生效
     */
    void setFadeOutTime(@IntRange(from = 1) int timeout);



    /**
     * 设置锁定状态
     *
     * @param locked 是否锁定
     */
    void setLocked(boolean locked);

    /**
     * 是否处于锁定状态
     */
    boolean isLocked();

    /**
     * 是否是全屏状态
     *
     * @return
     */
    boolean isFullScreen();

    /**
     * 横竖屏切换:用来代理{@link UNSVideoViewControl#toggleFullScreen()},即通过Controller调用VideoView的方法
     */
    boolean toggleFullScreen();

    /**
     * 开始全屏
     *
     * @return
     */
    default boolean startFullScreen() {
        return startFullScreen(false);
    }

    /**
     * 开始全屏:用来代理{@link UNSVideoViewControl#startFullScreen(boolean)}} ,即通过Controller调用VideoView的方法
     *
     * @return
     */
    boolean startFullScreen(boolean isLandscapeReversed);


    /**
     * 结束全屏:用来代理{@link UNSVideoViewControl#stopFullScreen()},即通过Controller调用VideoView的方法
     *
     * @return
     */
    boolean stopFullScreen();


    /**
     * 开始刷新进度
     */
    void startUpdateProgress();

    /**
     * 停止刷新进度
     */
    void stopUpdateProgress();




}
