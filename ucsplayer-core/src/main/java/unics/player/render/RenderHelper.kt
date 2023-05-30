package unics.player.render

import android.view.View
import androidx.annotation.IntRange

/**
 * render view 共同帮助类：统一部分行为
 */
class RenderHelper(private val view: View) {

    companion object {

        @JvmStatic
        fun create(view: View): RenderHelper {
            return RenderHelper(view)
        }
    }

    /**
     * 测量工具类
     */
    private val mMeasureHelper: RenderMeasureHelper = RenderMeasureHelper()

    /**
     * 获取宽的测量结果
     */
    val measuredWidth: Int get() = mMeasureHelper.measuredWidth

    /**
     * 测量所得的高
     */
    val measuredHeight: Int get() = mMeasureHelper.measuredHeight

    /**
     * 设置视频大小
     * @param videoWidth 视频宽
     * @param videoHeight 视频高
     */
    fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth == 0 || videoHeight == 0)
            return
        mMeasureHelper.setVideoSize(videoWidth, videoHeight)
        view.requestLayout()
    }

    /**
     * 设置缩放模式
     */
    fun setAspectRatioType(@AspectRatioType aspectRatioType: Int) {
        if (mMeasureHelper.aspectRatioType == aspectRatioType)
            return
        mMeasureHelper.aspectRatioType = aspectRatioType
        view.requestLayout()
    }

    /**
     * 设置旋转角度
     */
    fun setVideoRotation(@IntRange(from = 0, to = 360) degree: Int) {
        if (mMeasureHelper.videoRotationDegree == degree)
            return
        mMeasureHelper.videoRotationDegree = degree
        view.rotation = degree.toFloat()
    }

    /**
     * 设置镜像旋转
     */
    fun setMirrorRotation(enable: Boolean) {
        view.scaleX = if (enable) -1f else 1f
    }

    /**
     * 设置否是镜像旋转
     */
    fun isMirrorRotation(): Boolean {
        return view.scaleX == -1f
    }

    /**
     * 执行测量,通过[measuredWidth]、[measuredHeight]获取测量结果
     */
    fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}