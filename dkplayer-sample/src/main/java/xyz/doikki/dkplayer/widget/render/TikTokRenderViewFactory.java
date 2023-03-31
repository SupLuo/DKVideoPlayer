package xyz.doikki.dkplayer.widget.render;

import android.content.Context;

import droid.unicstar.videoplayer.render.Render;
import droid.unicstar.videoplayer.render.RenderFactory;
import droid.unicstar.videoplayer.render.TextureViewRender;

public class TikTokRenderViewFactory implements RenderFactory {

    public static TikTokRenderViewFactory create() {
        return new TikTokRenderViewFactory();
    }

    @Override
    public Render create(Context context) {
        return new TikTokRenderView(new TextureViewRender(context));
    }
}
