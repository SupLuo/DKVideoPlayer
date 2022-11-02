package xyz.doikki.dkplayer.activity.extend;

import android.view.View;

import xyz.doikki.dkplayer.R;
import xyz.doikki.dkplayer.activity.BaseActivity;
import xyz.doikki.dkplayer.util.DataUtil;
import xyz.doikki.scene.JustFullscreenPlayScene;
import xyz.doikki.videoplayer.DKVideoView;

/**
 * 全屏播放
 * Created by Doikki on 2017/4/21.
 *
 */

public class FullScreenActivity extends BaseActivity<DKVideoView> {

    private JustFullscreenPlayScene mScene ;

    @Override
    protected View getContentView() {
        mScene = JustFullscreenPlayScene.create(this);
        mScene.setControllerDefault(getString(R.string.str_fullscreen_directly));
        mVideoView = mScene.getVideoView();
        return null;
    }

    @Override
    protected int getTitleResId() {
        return R.string.str_fullscreen_directly;
    }

    @Override
    protected void initView() {
        super.initView();
        mVideoView.setDataSource(DataUtil.SAMPLE_URL);
        mVideoView.start();
    }

    @Override
    public void onBackPressed() {
        if(!mScene.onBackPressed())
            super.onBackPressed();
    }
}
