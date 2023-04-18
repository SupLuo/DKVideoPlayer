package xyz.doikki.dkplayer.widget.component;

import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.NonNull;

import droid.unicstar.player.UtilsKt;
import droid.unicstar.player.controller.MediaController;
import xyz.doikki.videoplayer.controller.component.ControlComponent;
import xyz.doikki.videoplayer.util.L;

public class PlayerMonitor implements ControlComponent {

    private MediaController mControlWrapper;

    @Override
    public void attachController(@NonNull MediaController controller) {
        mControlWrapper = controller;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        L.d("onVisibilityChanged: " + isVisible);
    }

    @Override
    public void onPlayStateChanged(int playState) {
        L.d("onPlayStateChanged: " + UtilsKt.playState2str(playState));
    }

    @Override
    public void onScreenModeChanged(int screenMode) {
        L.d("onPlayerStateChanged: " + UtilsKt.screenMode2str(screenMode));
    }

    @Override
    public void onProgressChanged(int duration, int position) {
        L.d("setProgress: duration: " + duration + " position: " + position + " buffered percent: " + mControlWrapper.getPlayerControl().getBufferedPercentage());
        L.d("network speed: " + mControlWrapper.getPlayerControl().getTcpSpeed());
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        L.d("onLockStateChanged: " + isLocked);
    }


}
