package xyz.doikki.dkplayer.activity.extend;

import android.view.View;

import xyz.doikki.dkplayer.R;
import xyz.doikki.dkplayer.activity.BaseActivity;
import xyz.doikki.dkplayer.util.DataUtil;
import xyz.doikki.dkplayer.ui.scene.JustFullscreenPlayScene;
import droid.unicstar.player.UCSVideoView;

/**
 * 全屏播放
 * Created by Doikki on 2017/4/21.
 *
 */

public class FullScreenActivity extends BaseActivity<UCSVideoView> {

    private JustFullscreenPlayScene mScene ;

    @Override
    protected View getContentView() {
        mScene = JustFullscreenPlayScene.create(this,true);
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
        mScene.onBackPressed();
    }
}
