package xyz.doikki.videoplayer.exo;

import android.content.Context;

import droid.unicstar.player.player.UCSPlayerFactory;

public class ExoMediaPlayerFactory implements UCSPlayerFactory<ExoMediaPlayer> {

    public static ExoMediaPlayerFactory create() {
        return new ExoMediaPlayerFactory();
    }

    @Override
    public ExoMediaPlayer create(Context context) {
        return new ExoMediaPlayer(context);
    }
}
