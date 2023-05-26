package unics.player.ijk;

import android.content.Context;

import unics.player.kernel.PlayerFactory;

public class IjkPlayerFactory implements PlayerFactory<IjkPlayer> {

    public static IjkPlayerFactory create() {
        return new IjkPlayerFactory();
    }

    @Override
    public IjkPlayer create(Context context) {
        return new IjkPlayer(context);
    }
}
