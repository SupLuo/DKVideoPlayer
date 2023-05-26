package unics.player.kernel;

import java.util.Locale;

public class PlayerException extends RuntimeException {

    private int what = UCSPlayer.MEDIA_ERROR_UNKNOWN;
    private int extra = UCSPlayer.MEDIA_ERROR_UNKNOWN;

    private PlayerException(int what, int extra) {
        super(String.format(Locale.getDefault(), "what=%d,extra=%d", what, extra));
        this.what = what;
        this.extra = extra;
    }

    public PlayerException(Throwable cause) {
        super(cause);
    }

    public int getWhat() {
        return what;
    }

    public int getExtra() {
        return extra;
    }

    public static PlayerException create(int what, int extra) {
        return new PlayerException(what, extra);
    }

}
