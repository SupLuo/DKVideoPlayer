package xyz.doikki.dkplayer.widget.render;

import android.content.Context;

import droid.unicstar.videoplayer.render.UNSRender;
import droid.unicstar.videoplayer.render.UNSRenderFactory;
import droid.unicstar.videoplayer.render.TextureViewRender;

public class TikTokRenderViewFactory implements UNSRenderFactory {

    public static TikTokRenderViewFactory create() {
        return new TikTokRenderViewFactory();
    }

    @Override
    public UNSRender create(Context context) {
        return new TikTokRenderView(new TextureViewRender(context));
    }
}
