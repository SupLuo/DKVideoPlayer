package xyz.doikki.videoplayer.exo;

import android.content.Context;

import unics.player.kernel.PlayerFactory;

public class ExoMediaPlayerFactory implements PlayerFactory<ExoMediaPlayer> {

    public static ExoMediaPlayerFactory create() {
        return new ExoMediaPlayerFactory();
    }

    @Override
    public ExoMediaPlayer create(Context context) {
        return new ExoMediaPlayer(context);
    }
}
