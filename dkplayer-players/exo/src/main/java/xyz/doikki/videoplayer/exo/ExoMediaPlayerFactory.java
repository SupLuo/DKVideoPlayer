package xyz.doikki.videoplayer.exo;

import android.content.Context;

import droid.unicstar.videoplayer.player.UNSPlayerFactory;

public class ExoMediaPlayerFactory implements UNSPlayerFactory<ExoMediaPlayer> {

    public static ExoMediaPlayerFactory create() {
        return new ExoMediaPlayerFactory();
    }

    @Override
    public ExoMediaPlayer create(Context context) {
        return new ExoMediaPlayer(context);
    }
}
