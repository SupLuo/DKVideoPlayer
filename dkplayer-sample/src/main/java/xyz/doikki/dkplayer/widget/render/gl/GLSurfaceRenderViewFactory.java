package xyz.doikki.dkplayer.widget.render.gl;

import android.content.Context;

import droid.unicstar.player.render.UCSRender;
import droid.unicstar.player.render.UCSRenderFactory;

public class GLSurfaceRenderViewFactory implements UCSRenderFactory {

    public static GLSurfaceRenderViewFactory create() {
        return new GLSurfaceRenderViewFactory();
    }

    @Override
    public UCSRender create(Context context) {
        return new GLSurfaceRenderView(context);
    }
}
