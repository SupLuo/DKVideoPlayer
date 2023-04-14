package xyz.doikki.dkplayer.fragment.list;

import droid.unicstar.videoplayer.player.UNSPlayer;
import xyz.doikki.dkplayer.util.Utils;
import xyz.doikki.dkplayer.widget.controller.PortraitWhenFullScreenController;
import xyz.doikki.videocontroller.component.CompleteView;
import xyz.doikki.videocontroller.component.ErrorView;
import xyz.doikki.videocontroller.component.GestureView;
import xyz.doikki.videocontroller.component.TitleView;
import droid.unicstar.videoplayer.UNSVideoView;

/**
 * 全屏后手动横屏，并不完美，仅做参考
 */
public class RecyclerViewPortraitFragment extends RecyclerViewAutoPlayFragment {

    @Override
    protected void initVideoView() {
        mVideoView = new UNSVideoView(getActivity());
        mVideoView.addOnPlayStateChangeListener(new UNSPlayer.OnPlayStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                if (playState == UNSPlayer.STATE_IDLE) {
                    Utils.removeViewFormParent(mVideoView);
                    mLastPos = mCurPos;
                    mCurPos = -1;
                }
            }
        });
        mController = new PortraitWhenFullScreenController(getActivity());
        mErrorView = new ErrorView(getActivity());
        mController.addControlComponent(mErrorView);
        mCompleteView = new CompleteView(getActivity());
        mController.addControlComponent(mCompleteView);
        mTitleView = new TitleView(getActivity());
        mController.addControlComponent(mTitleView);
        mController.addControlComponent(new GestureView(getActivity()));
        mVideoView.setEnableOrientationSensor(true);
        mVideoView.setVideoController(mController);
    }

    @Override
    public void onItemChildClick(int position) {
        mVideoView.startVideoViewFullScreen();
        super.onItemChildClick(position);
    }
}
