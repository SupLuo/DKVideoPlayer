package unics.player.control.leanback

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import droid.unicstar.player.ui.setTextOrGone
import unics.player.controller.ControlComponent
import unics.player.controller.MediaController
import unics.player.kernel.UCSPlayer
import xyz.doikki.videocontroller.R

/**
 * Created by Lucio on 2021/4/15.
 * 提供一个居中的横向进度条，进度条的上方一个大文本，下方一个小文本
 *
 * 作用：适合在TV上使用
 * 1、在开始播放前可以作为数据加载的缓冲界面，显示视频的标题以及上次观看到什么位置等，其中大文本=[textBeforePlaying]，小文本[hintBeforePlaying]
 * 2、作为缓冲的显示界面，其中大文本=[textWhenBuffering],小文本[hintWhenBuffering]
 */
class PrepareControlComponentLeanback @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ControlComponent {

    private val mTitleView: TextView
    private val mHintView: TextView
    private val mLoadingView: ProgressBar
    private var mController: MediaController? = null

    //缓冲过程中显示的大文本
    @JvmField
    var textWhenBuffering: CharSequence? = null

    //缓冲过程中显示的提示语
    @JvmField
    var hintWhenBuffering: CharSequence? = null

    //开始播放前显示的大文本
    @JvmField
    var textBeforePlaying: CharSequence? = null

    //开始播放前显示的提示语：比如显示上次观看到什么位置等
    @JvmField
    var hintBeforePlaying: CharSequence? = null

    private var mCurrentText: CharSequence? = null
    private var mCurrentHint: CharSequence? = null

    /**
     * 显示播放前的状态
     */
    fun showBeforePlaying(
        text: CharSequence? = textBeforePlaying,
        hint: CharSequence? = hintBeforePlaying
    ) {
        textBeforePlaying = text
        hintBeforePlaying = hint
        show(text, hint)
    }

    /**
     * 显示缓冲中的状态
     */
    fun showWhenBuffering(
        text: CharSequence? = textWhenBuffering,
        hint: CharSequence? = hintWhenBuffering
    ) {
        textWhenBuffering = text
        hintWhenBuffering = hint
        show(text, hint)
    }

    fun show(text: CharSequence?, hint: CharSequence?) {
        if (mCurrentText != text) {
            mTitleView.setTextOrGone(text)
            mCurrentText = text
        }
        if (mCurrentHint != hint) {
            mHintView.setTextOrGone(hint)
            mCurrentHint = hint
        }
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    override fun onControllerAttached(controller: MediaController) {
        mController = controller
    }

    override fun getView(): View {
        return this
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            UCSPlayer.STATE_PREPARING, UCSPlayer.STATE_PREPARED -> {
                show(textBeforePlaying, hintBeforePlaying)
            }
            UCSPlayer.STATE_BUFFERING -> {
                show(textWhenBuffering, hintWhenBuffering)
            }
            UCSPlayer.STATE_IDLE,
            UCSPlayer.STATE_ERROR,
            UCSPlayer.STATE_PLAYBACK_COMPLETED,
            UCSPlayer.STATE_PREPARED_BUT_ABORT,
            UCSPlayer.STATE_PLAYING,
            UCSPlayer.STATE_PAUSED,
            UCSPlayer.STATE_BUFFERED -> {
                hide()
            }
        }
    }

    init {
        //设置默认参数
        if (attrs == null) {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.color.ucsp_ctrl_control_component_background_color_opacity)
        }
        View.inflate(context, R.layout.ucsp_ctrl_prepare_control_component_leanback, this)
        mTitleView = findViewById(R.id.ucsp_ctrl_text)
        mHintView = findViewById(R.id.ucsp_ctrl_hint)
        mLoadingView = findViewById(R.id.ucsp_ctrl_loading)
        textWhenBuffering = context.resources.getString(R.string.ucsp_ctrl_str_buffering)
    }

}