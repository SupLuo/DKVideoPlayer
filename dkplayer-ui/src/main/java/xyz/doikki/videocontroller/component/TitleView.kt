package xyz.doikki.videocontroller.component

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import xyz.doikki.videocontroller.R
import droid.unicstar.player.ui.TVCompatible
import unics.player.ScreenMode
import unics.player.control.BaseControlComponent
import unics.player.kernel.UCSPlayer

/**
 * 播放器顶部标题栏
 */
@TVCompatible(message = "没指定布局id时，TV上运行和手机上运行会加载不同的默认布局，tv的布局不包含电量和返回按钮逻辑")
class TitleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    @LayoutRes layoutId: Int = UNDEFINED_LAYOUT
) : BaseControlComponent(context, attrs, defStyleAttr) {

    private val mTitleContainer: LinearLayout
    private val mTitle: TextView
//    private val mSysTime: TextView//系统当前时间

    private lateinit var mBatteryReceiver: BatteryReceiver

    //是否注册BatteryReceiver
    private var mBatteryReceiverRegistered = false

    /**
     * 是否启用电量检测功能
     */
    private var mBatteryEnabled: Boolean = true

    fun setTitle(title: CharSequence?) {
        mTitle.text = title
    }

    fun setOnBackClickListener(listener: OnClickListener?){
        findViewById<View?>(R.id.back)?.setOnClickListener (listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mBatteryEnabled && mBatteryReceiverRegistered) {
            context.unregisterReceiver(mBatteryReceiver)
            mBatteryReceiverRegistered = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mBatteryEnabled && !mBatteryReceiverRegistered) {
            context.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            mBatteryReceiverRegistered = true
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        //只在全屏时才有效
        if (mController?.isFullScreen != true) return
        if (isVisible) {
            if (visibility == GONE) {
//                mSysTime.text = PlayerUtils.getCurrentSystemTime()
                visibility = VISIBLE
                anim?.let { startAnimation(it) }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                anim?.let { startAnimation(it) }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            UCSPlayer.STATE_IDLE, UCSPlayer.STATE_PREPARED_BUT_ABORT,
            UCSPlayer.STATE_PREPARING, UCSPlayer.STATE_PREPARED,
            UCSPlayer.STATE_ERROR, UCSPlayer.STATE_PLAYBACK_COMPLETED -> visibility = GONE
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onScreenModeChanged(screenMode: Int) {
        val controller = this.mController
        if (screenMode == ScreenMode.FULL_SCREEN) {
            if (controller != null && controller.isShowing && !controller.isLocked) {
                visibility = VISIBLE
//                mSysTime.text = PlayerUtils.getCurrentSystemTime()
            }
            mTitle.isSelected = true
        } else {
            visibility = GONE
            mTitle.isSelected = false
        }
        val activity = this.activity ?: return
        containerControl?.let {
                containerControl->
            if (containerControl.hasCutout()) {
                val orientation = activity.requestedOrientation
                val cutoutHeight = containerControl.getCutoutHeight()
                when (orientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                        mTitleContainer.setPadding(0, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                        mTitleContainer.setPadding(cutoutHeight, 0, 0, 0)
                    }
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                        mTitleContainer.setPadding(0, 0, cutoutHeight, 0)
                    }
                }
            }
        }

    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (isLocked) {
            visibility = GONE
        } else {
            visibility = VISIBLE
//            mSysTime.text = PlayerUtils.getCurrentSystemTime()
        }
    }

    private class BatteryReceiver(private val pow: ImageView) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val current = extras.getInt("level") // 获得当前电量
            val total = extras.getInt("scale") // 获得总电量
            val percent = current * 100 / total
            pow.drawable.level = percent
        }
    }

    init {
        visibility = GONE
        val isTelevisionUiMode = isTelevisionUiMode
        if (layoutId > 0) {
            layoutInflater.inflate(layoutId, this)
        } else {
            layoutInflater.inflate(
                if (isTelevisionUiMode) R.layout.dkplayer_layout_title_view_tv else R.layout.dkplayer_layout_title_view,
                this
            )
        }

        if (isTelevisionUiMode) {
            mBatteryEnabled = false
            //tv模式不要电量，不要返回按钮
            findViewById<View>(R.id.back)?.visibility = GONE
            findViewById<View>(R.id.iv_battery)?.visibility = GONE
        } else {
            mBatteryEnabled = true
            findViewById<View?>(R.id.back)?.setOnClickListener {
                val activity = activity
                if (activity != null && mController?.isFullScreen == true) {
                    mController?.stopFullScreen()
                }
            }
            //电量
            val batteryLevel = findViewById<ImageView>(R.id.iv_battery)
            mBatteryReceiver = BatteryReceiver(batteryLevel)
        }
        mTitleContainer = findViewById(R.id.title_container)
        mTitle = findViewById(R.id.title)
//        mSysTime = findViewById(R.id.sys_time)
    }
}