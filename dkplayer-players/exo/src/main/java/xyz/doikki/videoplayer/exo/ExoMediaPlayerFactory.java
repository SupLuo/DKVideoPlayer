package xyz.doikki.videoplayer.exo;

import android.content.Context;

import unics.player.kernel.UCSPlayerFactory;

public class ExoMediaPlayerFactory implements UCSPlayerFactory<ExoMediaPlayer> {

    public static ExoMediaPlayerFactory create() {
        return new ExoMediaPlayerFactory();
    }

    @Override
    public ExoMediaPlayer create(Context context) {
        return new ExoMediaPlayer(context);
    }
}
