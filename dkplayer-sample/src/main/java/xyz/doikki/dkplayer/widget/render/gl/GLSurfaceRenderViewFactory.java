package xyz.doikki.dkplayer.widget.render.gl;

import android.content.Context;

import droid.unicstar.videoplayer.render.Render;
import droid.unicstar.videoplayer.render.RenderFactory;

public class GLSurfaceRenderViewFactory implements RenderFactory {

    public static GLSurfaceRenderViewFactory create() {
        return new GLSurfaceRenderViewFactory();
    }

    @Override
    public Render create(Context context) {
        return new GLSurfaceRenderView(context);
    }
}
