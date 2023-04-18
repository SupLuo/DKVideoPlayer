package xyz.doikki.dkplayer.widget.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import droid.unicstar.player.UCSDisplayContainer;
import droid.unicstar.player.UCSPlayerProxy;
import droid.unicstar.player.UtilsKt;
import droid.unicstar.player.controller.MediaController;
import droid.unicstar.player.controller.UCSContainerControl;
import droid.unicstar.player.controller.UCSPlayerControl;
import xyz.doikki.videoplayer.controller.component.ControlComponent;

/**
 * 调试信息
 */
public class DebugInfoView extends AppCompatTextView implements ControlComponent {

    private MediaController mController;

    public DebugInfoView(Context context) {
        super(context);
    }

    public DebugInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DebugInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        setBackgroundResource(android.R.color.black);
        setTextSize(10);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        setLayoutParams(lp);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {

    }

    @Override
    public void onPlayStateChanged(int playState) {
        setText(getDebugString(playState));
        bringToFront();
    }

    /**
     * Returns the debugging information string to be shown by the target {@link TextView}.
     */
    protected String getDebugString(int playState) {
        UCSPlayerControl control = mController.getPlayerControl();
        UCSContainerControl containerControl = mController.getContainerControl();
        if (control == null)
            return "";
        int[] videoSize = containerControl.getVideoSize();
        StringBuilder sb = new StringBuilder();
        if (control instanceof UCSPlayerProxy) {
            sb.append("player:").append(((UCSPlayerProxy) control).getPlayerName()).append("   ");
        }

        if (containerControl instanceof UCSDisplayContainer) {
            sb.append("render:").append(((UCSDisplayContainer) containerControl).getRenderName()).append("\n");
        }
        sb.append(UtilsKt.playState2str(playState)).append("  ")
                .append("video width:").append(videoSize[0])
                .append(",height:").append(videoSize[1]);
        return sb.toString();
    }

    @Override
    public void onScreenModeChanged(int screenMode) {
        bringToFront();
    }

    @Override
    public void onProgressChanged(int duration, int position) {

    }

    @Override
    public void onLockStateChanged(boolean isLocked) {

    }

    @Override
    public void attachController(@NonNull MediaController controller) {
        this.mController = controller;
    }
}
