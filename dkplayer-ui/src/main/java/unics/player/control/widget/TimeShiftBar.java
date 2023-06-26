package unics.player.control.widget;


import android.view.ViewGroup;
import android.view.animation.Animation;

public interface TimeShiftBar {
    int getProgress();

    void setProgress(int progress);

    int getSecondaryProgress();

    void setSecondaryProgress(int secondaryProgress);

    int getMax();

    void setMax(int max);

    void setVisibility(int visibility);

    int getVisibility();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void startAnimation(Animation animation);

    void setPadding(int left, int top, int right, int bottom);

    ViewGroup.LayoutParams getLayoutParams();


    void setLayoutParams(ViewGroup.LayoutParams params);
}
