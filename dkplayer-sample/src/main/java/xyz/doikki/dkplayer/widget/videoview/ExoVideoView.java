package xyz.doikki.dkplayer.widget.videoview;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;

import java.util.Map;

import xyz.doikki.dkplayer.widget.player.CustomExoMediaPlayer;
import droid.unicstar.videoplayer.player.UNSPlayer;
import droid.unicstar.videoplayer.player.UNSPlayerFactory;
import droid.unicstar.videoplayer.UNSVideoView;
import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;

public class ExoVideoView extends UNSVideoView {

    private MediaSource mMediaSource;

    private boolean mIsCacheEnabled;

    private LoadControl mLoadControl;
    private RenderersFactory mRenderersFactory;
    private TrackSelector mTrackSelector;

    private final ExoMediaSourceHelper mHelper;

    public ExoVideoView(Context context) {
        super(context);
    }

    public ExoVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    {
        //由于传递了泛型，必须将CustomExoMediaPlayer设置进来，否者报错
        setPlayerFactory(new UNSPlayerFactory<CustomExoMediaPlayer>() {
            @Override
            public CustomExoMediaPlayer create(Context context) {
                CustomExoMediaPlayer player =  new CustomExoMediaPlayer(context);
                player.setLoadControl(mLoadControl);
                player.setRenderersFactory(mRenderersFactory);
                player.setTrackSelector(mTrackSelector);
                return player;
            }
        });
        mHelper = ExoMediaSourceHelper.getInstance(getContext());
    }

    private CustomExoMediaPlayer mediaPlayer() {
        return (CustomExoMediaPlayer) getKernel();
    }

//    @Override
//    protected void prepareKernelDataSource() {
//        if (mMediaSource != null) {
//            mediaPlayer().setDataSource(mMediaSource);
//            mediaPlayer().prepareAsync();
//            setCurrentState(STATE_PREPARING);
//        }
//    }

    /**
     * 设置ExoPlayer的MediaSource
     */
    public void setMediaSource(MediaSource mediaSource) {
        mMediaSource = mediaSource;
    }


//    @Override
//    public void setDataSource(@NonNull String path, @Nullable Map<String, String> headers) {
//        mMediaSource = mHelper.getMediaSource(path, headers, mIsCacheEnabled);
//    }


    /**
     * 是否打开缓存
     */
    public void setCacheEnabled(boolean isEnabled) {
        mIsCacheEnabled = isEnabled;
    }

    public void setLoadControl(LoadControl loadControl) {
        mLoadControl = loadControl;
    }

    public void setRenderersFactory(RenderersFactory renderersFactory) {
        mRenderersFactory = renderersFactory;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
        mTrackSelector = trackSelector;
    }
}
