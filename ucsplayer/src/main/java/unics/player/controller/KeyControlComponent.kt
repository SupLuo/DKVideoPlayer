package unics.player.controller

import android.view.KeyEvent

interface KeyControlComponent : ControlComponent {

    /**
     * 开始按住左右方向键拖动位置
     */
    fun onStartLeftOrRightKeyPressedForSeeking(event: KeyEvent){}

    /**
     * 停止按住左右方向键拖动位置
     */
    fun onStopLeftOrRightKeyPressedForSeeking(event: KeyEvent){}

    /**
     * 取消方向键拖动位置
     */
    fun onCancelLeftOrRightKeyPressedForSeeking(keyEvent: KeyEvent){}


    /**
     * 滑动调整进度
     * @param slidePosition 滑动进度
     * @param currentPosition 当前播放进度
     * @param duration 视频总长度
     */
    fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int)

}