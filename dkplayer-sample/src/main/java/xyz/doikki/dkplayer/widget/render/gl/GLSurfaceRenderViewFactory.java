package xyz.doikki.dkplayer.widget.render.gl;

import android.content.Context;

import droid.unicstar.videoplayer.render.UNSRender;
import droid.unicstar.videoplayer.render.UNSRenderFactory;

public class GLSurfaceRenderViewFactory implements UNSRenderFactory {

    public static GLSurfaceRenderViewFactory create() {
        return new GLSurfaceRenderViewFactory();
    }

    @Override
    public UNSRender create(Context context) {
        return new GLSurfaceRenderView(context);
    }
}
