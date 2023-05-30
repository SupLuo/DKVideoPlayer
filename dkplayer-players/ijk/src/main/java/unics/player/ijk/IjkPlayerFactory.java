package unics.player.ijk;

import android.content.Context;

import unics.player.kernel.UCSPlayerFactory;

public class IjkPlayerFactory implements UCSPlayerFactory<IjkPlayer> {

    public static IjkPlayerFactory create() {
        return new IjkPlayerFactory();
    }

    @Override
    public IjkPlayer create(Context context) {
        return new IjkPlayer(context);
    }
}
