package xyz.doikki.dkplayer.activity.extend;

import android.view.View;

import xyz.doikki.dkplayer.R;
import xyz.doikki.dkplayer.activity.BaseActivity;
import xyz.doikki.dkplayer.util.DataUtil;
import xyz.doikki.videocontroller.component.TitleView;
import xyz.doikki.videocontroller.scene.JustFullscreenPlayScene;
import xyz.doikki.videoplayer.DKVideoView;

/**
 * 全屏播放
 * Created by Doikki on 2017/4/21.
 */

public class FullScreenActivity extends BaseActivity<DKVideoView> {

    private JustFullscreenPlayScene mScene;


    @Override
    protected View getContentView() {
        mScene = JustFullscreenPlayScene.bind(this);
        mVideoView = mScene.getDkVideoView();
        return null;
    }

    @Override
    protected int getTitleResId() {
        return R.string.str_fullscreen_directly;
    }

    @Override
    protected void initView() {
        super.initView();
        mVideoView.startVideoViewFullScreen();
        mVideoView.setDataSource(DataUtil.SAMPLE_URL);

        TitleView titleView = new TitleView(this);
        // 我这里改变了返回按钮的逻辑，我不推荐这样做，我这样只是为了方便，
        // 如果你想对某个组件进行定制，直接将该组件的代码复制一份，改成你想要的样子
        titleView.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        titleView.setTitle(getString(R.string.str_fullscreen_directly));
        mScene.getController().addControlComponent(titleView);
        mVideoView.start();
    }

    @Override
    public void onBackPressed() {
        if (!mScene.getController().isLocked()) {
            finish();
        }
    }
}
