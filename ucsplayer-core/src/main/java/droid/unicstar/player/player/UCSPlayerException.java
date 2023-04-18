package droid.unicstar.player.player;

import android.annotation.SuppressLint;

public class UCSPlayerException extends RuntimeException {

    private int what = UCSPlayer.MEDIA_ERROR_UNKNOWN;
    private int extra = UCSPlayer.MEDIA_ERROR_UNKNOWN;

    @SuppressLint("DefaultLocale")
    public UCSPlayerException(int what, int extra) {
        super(String.format("what=%d,extra=%d", what, extra));
        this.what = what;
        this.extra = extra;
    }

    public UCSPlayerException(Throwable cause) {
        super(cause);
    }

    public int getWhat() {
        return what;
    }

    public int getExtra() {
        return extra;
    }

}
