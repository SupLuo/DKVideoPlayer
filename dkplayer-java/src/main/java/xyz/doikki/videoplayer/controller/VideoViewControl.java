package xyz.doikki.videoplayer.controller;

import droid.unicstar.videoplayer.controller.UNSContainerControl;
import droid.unicstar.videoplayer.controller.UNSPlayerControl;

/**
 * 播放控制类：用于提供给Controller控制播放器（VideoView）
 * 在{@link UNSPlayerControl}功能的基础上新增了视图方面的控制
 */
public interface VideoViewControl extends UNSPlayerControl, UNSContainerControl {

    /**********[Start]全屏、小窗口、自动横屏等 屏幕相关操作所需接口***********/
//    /**
//     * 是否是全屏状态
//     *
//     * @return
//     */
//    boolean isFullScreen();

//    /**
//     * 当前是否是小窗播放状态
//     *
//     * @return
//     */
//    boolean isTinyScreen();
//
//    /**
//     * 横竖屏切换
//     */
//    boolean toggleFullScreen();
//
//    default boolean startFullScreen(){
//        return startFullScreen(false);
//    }
//
//    /**
//     * 开始全屏;
//     * 屏幕切换之后会回调{@link UNSVideoView.OnStateChangeListener#onScreenModeChanged(int)}
//     * @param isLandscapeReversed 是否是反向横屏
//     * @see UNSVideoView#addOnStateChangeListener(UNSVideoView.OnStateChangeListener)
//     */
//    boolean startFullScreen(boolean isLandscapeReversed);
//
//    /**
//     * 结束全屏
//     * 屏幕切换之后会回调{@link UNSVideoView.OnStateChangeListener#onScreenModeChanged(int)}
//     */
//    boolean stopFullScreen();
//
//    /**
//     * 开始VideoView全屏（用于只想横竖屏切换VideoView而不更改Activity方向的情况）
//     * 此方法与{@link #startFullScreen()}方法的区别在于，此方法不会调用{@link android.app.Activity#setRequestedOrientation(int)}改变Activity的方向。
//     *
//     * @return true：video view的方向发生了变化
//     */
//    boolean startVideoViewFullScreen();
//
//    /**
//     * 结束VideoView的全屏（用于只想横竖屏切换VideoView而不更改Activity方向的情况）
//     * 此方法与{@link #startFullScreen()}方法的区别在于，此方法不会调用{@link android.app.Activity#setRequestedOrientation(int)}改变Activity的方向。
//     *
//     * @return true：video view的方向发生了变化
//     */
//    boolean stopVideoViewFullScreen();
//
//    /**
//     * 开始小窗播放
//     * 屏幕切换之后会回调{@link UNSVideoView.OnStateChangeListener#onScreenModeChanged(int)}
//     */
//    void startTinyScreen();
//
//    /**
//     * 结束小窗播放
//     * 屏幕切换之后会回调{@link UNSVideoView.OnStateChangeListener#onScreenModeChanged(int)}
//     */
//    void stopTinyScreen();

    /**********[END]全屏、小窗口、自动横屏等 屏幕相关操作所需接口***********/

    /**
     * 设置静音
     *
     * @param isMute true:静音 false：相反
     */
    void setMute(boolean isMute);

    /**
     * 当前是否静音
     *
     * @return true:静音 false：相反
     */
    boolean isMute();



    /**
     * 获取图像宽高
     *
     * @return 0-width 1-height
     */
    int[] getVideoSize();


    /*以下方法还未梳理*/

    /**
     * 获取缓冲网速：只有IJK播放器支持
     *
     * @return
     */
    long getTcpSpeed();


}