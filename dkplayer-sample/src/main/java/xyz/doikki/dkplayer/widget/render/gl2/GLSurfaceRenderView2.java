package xyz.doikki.dkplayer.widget.render.gl2;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import droid.unicstar.player.render.internal.RenderHelper;
import xyz.doikki.dkplayer.widget.render.gl2.chooser.GLConfigChooser;
import xyz.doikki.dkplayer.widget.render.gl2.contextfactory.GLContextFactory;
import xyz.doikki.dkplayer.widget.render.gl2.filter.GlFilter;
import droid.unicstar.player.player.UCSPlayer;
import droid.unicstar.player.render.UCSRender;

public class GLSurfaceRenderView2 extends GLSurfaceView implements UCSRender {

    private final GLVideoRenderer renderer;

    private RenderHelper mHelper = RenderHelper.create(this);

    public GLSurfaceRenderView2(Context context) {
        this(context, null);
    }

    public GLSurfaceRenderView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextFactory(new GLContextFactory());
        setEGLConfigChooser(new GLConfigChooser());
        renderer = new GLVideoRenderer(this);
        setRenderer(renderer);
    }


    @Override
    public void bindPlayer(@NonNull UCSPlayer player) {
        this.renderer.setPlayer(player);
    }

    @Override
    public void setVideoSize(int videoWidth, int videoHeight) {
        mHelper.setVideoSize(videoWidth, videoHeight);
    }


    @Override
    public void setVideoRotation(int degree) {
        mHelper.setVideoRotation(degree);
    }

    @Override
    public void setAspectRatioType(int aspectRatioType) {
        mHelper.setAspectRatioType(aspectRatioType);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void screenshot(boolean highQuality, @NonNull ScreenShotCallback callback) {
        //todo glsurface 是可以截图的，待处理
        callback.onScreenShotResult(null);
    }

    @Override
    public void setSurfaceListener(SurfaceListener listener) {
        //todo
    }

    @Override
    public void release() {

    }

    public void setGlFilter(GlFilter glFilter) {
        renderer.setGlFilter(glFilter);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mHelper.doMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mHelper.getMeasuredWidth(), mHelper.getMeasuredHeight());
    }

    @Override
    public void onPause() {
        super.onPause();
        renderer.release();
    }
}
