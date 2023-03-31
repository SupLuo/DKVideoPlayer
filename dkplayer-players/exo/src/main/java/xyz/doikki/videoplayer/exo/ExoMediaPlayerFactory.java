package xyz.doikki.videoplayer.exo;

import android.content.Context;

import droid.unicstar.videoplayer.player.CSPlayerFactory;

public class ExoMediaPlayerFactory implements CSPlayerFactory<ExoMediaPlayer> {

    public static ExoMediaPlayerFactory create() {
        return new ExoMediaPlayerFactory();
    }

    @Override
    public ExoMediaPlayer create(Context context) {
        return new ExoMediaPlayer(context);
    }
}
