package xyz.doikki.videoplayer.ijk;

import android.content.Context;

import droid.unicstar.player.player.UCSPlayerFactory;

public class IjkPlayerFactory implements UCSPlayerFactory<IjkDKPlayer> {

    public static IjkPlayerFactory create() {
        return new IjkPlayerFactory();
    }

    @Override
    public IjkDKPlayer create(Context context) {
        return new IjkDKPlayer(context);
    }
}
