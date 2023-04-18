package xyz.doikki.dkplayer.widget.render;

import android.content.Context;

import droid.unicstar.player.render.UCSRender;
import droid.unicstar.player.render.UCSRenderFactory;
import droid.unicstar.player.render.TextureViewRender;

public class TikTokRenderViewFactory implements UCSRenderFactory {

    public static TikTokRenderViewFactory create() {
        return new TikTokRenderViewFactory();
    }

    @Override
    public UCSRender create(Context context) {
        return new TikTokRenderView(new TextureViewRender(context));
    }
}
