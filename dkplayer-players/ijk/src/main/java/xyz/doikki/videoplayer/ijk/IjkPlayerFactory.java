package xyz.doikki.videoplayer.ijk;

import android.content.Context;

import droid.unicstar.videoplayer.player.UNSPlayerFactory;

public class IjkPlayerFactory implements UNSPlayerFactory<IjkDKPlayer> {

    public static IjkPlayerFactory create() {
        return new IjkPlayerFactory();
    }

    @Override
    public IjkDKPlayer create(Context context) {
        return new IjkDKPlayer(context);
    }
}
