package droid.unicstar.videoplayer.player;

import android.annotation.SuppressLint;

public class CSPlayerException extends RuntimeException {

    private int what = CSPlayer.MEDIA_ERROR_UNKNOWN;
    private int extra = CSPlayer.MEDIA_ERROR_UNKNOWN;

    @SuppressLint("DefaultLocale")
    public CSPlayerException(int what, int extra) {
        super(String.format("what=%d,extra=%d", what, extra));
        this.what = what;
        this.extra = extra;
    }

    public CSPlayerException(Throwable cause) {
        super(cause);
    }

    public int getWhat() {
        return what;
    }

    public int getExtra() {
        return extra;
    }

}
