package xyz.doikki.dkplayer.activity.api

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import droid.unicstar.videoplayer.UNSDisplayContainer
import droid.unicstar.videoplayer.UNSPlayerProxy
import droid.unicstar.videoplayer.UNSVideoView
import droid.unicstar.videoplayer.player.UNSPlayer
import droid.unicstar.videoplayer.render.AspectRatioType
import droid.unicstar.videoplayer.render.UNSRenderFactory
import droid.unicstar.player.usecase.DoublePlayerUseCase
import droid.unicstar.player.usecase.UCSUseCase
import xyz.doikki.dkplayer.R
import xyz.doikki.dkplayer.activity.BaseActivity
import xyz.doikki.dkplayer.util.IntentKeys
import xyz.doikki.dkplayer.widget.component.DebugInfoView
import xyz.doikki.dkplayer.widget.component.PlayerMonitor
import xyz.doikki.videocontroller.StandardVideoController
import xyz.doikki.videocontroller.TVVideoController
import xyz.doikki.videocontroller.component.*
import xyz.doikki.videoplayer.util.L

/**
 * 播放器演示
 * Created by Doikki on 2017/4/7.
 */
class Player2ShareRenderActivity : BaseActivity<UNSVideoView>() {

    private lateinit var controller: StandardVideoController

    private val mPlay1: UNSPlayerProxy by lazy {
        UNSPlayerProxy(this)
    }
    private val mPlay2: UNSPlayerProxy by lazy {
        UNSPlayerProxy(this)
    }
    private lateinit var mPlayerContainer: UNSDisplayContainer

    private val mScene: DoublePlayerUseCase by lazy {
        UCSUseCase.newDoublePlayerScene(mPlay1, mPlay2, mPlayerContainer).also {
            it.bindLifecycleOwner(this)
        }
    }

    override fun getLayoutResId() = R.layout.activity_player2_share_render

    override fun initView() {
        super.initView()
        mPlayerContainer = findViewById(R.id.player_container)
        //根据屏幕方向自动进入/退出全屏
        mPlayerContainer.setEnableOrientationSensor(true)
        intent?.let {
            controller = TVVideoController(this)

            val prepareView = PrepareView(this) //准备播放界面
            prepareView.setClickStart()
            val thumb = prepareView.findViewById<ImageView>(R.id.thumb) //封面图
            Glide.with(this).load(THUMB).into(thumb)
            controller.addControlComponent(prepareView)
            controller.addControlComponent(CompleteView(this)) //自动完成播放界面
            controller.addControlComponent(ErrorView(this)) //错误界面
            val titleView = TitleView(this) //标题栏
            controller.addControlComponent(titleView)

            //根据是否为直播设置不同的底部控制条
            val isLive = it.getBooleanExtra(IntentKeys.IS_LIVE, false)
            if (isLive) {
                controller.addControlComponent(LiveControlView(this)) //直播控制条
            } else {
                val vodControlView = VodControlView(this) //点播控制条
                //是否显示底部进度条。默认显示
//                vodControlView.showBottomProgress(false);
                controller.addControlComponent(vodControlView)
            }
            val gestureControlView = GestureView(this) //滑动控制视图
            controller.addControlComponent(gestureControlView)
            //根据是否为直播决定是否需要滑动调节进度
            controller.seekEnabled = !isLive

            //设置标题
            val title = it.getStringExtra(IntentKeys.TITLE)
            titleView.setTitle(title)

            //在控制器上显示调试信息
            controller.addControlComponent(DebugInfoView(this))
            //在LogCat显示调试信息
            controller.addControlComponent(PlayerMonitor())

            //如果你不想要UI，不要设置控制器即可
            mPlayerContainer.setVideoController(controller)
            val url = it.getStringExtra(IntentKeys.URL)
            val url2 = it.getStringExtra(IntentKeys.URL2)
            mPlay1.setDataSource(this, url!!)
            mPlay2.setDataSource(this, url2!!)
            mScene.playOne()
            mPlay1.addOnPlayStateChangeListener(mOnStateChangeListener)
            mPlay2.addOnPlayStateChangeListener(mOnStateChangeListener)
        }
        loadingAssistRunnable.run()
    }

    private var lastBTime = System.currentTimeMillis()
    private var isBuff = false
    private val handler = Handler()
    private val loadingAssistRunnable = object : Runnable {
        override fun run() {
            try {
                if (isBuff && System.currentTimeMillis() - lastBTime >= 20 * 1000) {
                    println("补全代码")
                    controller.replay(false)
                    lastBTime = System.currentTimeMillis()
                    isBuff = false
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val mOnStateChangeListener: UNSPlayer.OnPlayStateChangeListener =
        UNSPlayer.OnPlayStateChangeListener { playState ->
            when (playState) {
                UNSPlayer.STATE_IDLE -> {
                }
                UNSPlayer.STATE_PREPARING -> {
                }
                UNSPlayer.STATE_PREPARED -> {
                }
                UNSPlayer.STATE_PLAYING -> {
                    //需在此时获取视频宽高
                    val videoSize = mPlayerContainer.getVideoSize()
                    L.d("视频宽：" + videoSize[0])
                    L.d("视频高：" + videoSize[1])
                }
                UNSPlayer.STATE_PAUSED -> {
                }
                UNSPlayer.STATE_BUFFERING -> {
                    isBuff = true
                    lastBTime = System.currentTimeMillis()
                }
                UNSPlayer.STATE_BUFFERED -> {
                    isBuff = false
                }
                UNSPlayer.STATE_PLAYBACK_COMPLETED -> {
                }
                UNSPlayer.STATE_ERROR -> {
                }
            }
        }
    private var i = 0
    fun onButtonClick(view: View) {
        when (view.id) {
            R.id.use_play1 -> {
                mScene.playOne()
            }
            R.id.use_play2 -> {
                mScene.playTwo()
            }
            R.id.scale_default -> mPlayerContainer.setAspectRatioType(AspectRatioType.DEFAULT_SCALE)
            R.id.scale_189 -> mPlayerContainer.setAspectRatioType(AspectRatioType.SCALE_18_9)
            R.id.scale_169 -> mPlayerContainer.setAspectRatioType(AspectRatioType.SCALE_16_9)
            R.id.scale_43 -> mPlayerContainer.setAspectRatioType(AspectRatioType.SCALE_4_3)
            R.id.scale_original -> mPlayerContainer.setAspectRatioType(AspectRatioType.SCALE_ORIGINAL)
            R.id.scale_match_parent -> mPlayerContainer.setAspectRatioType(AspectRatioType.MATCH_PARENT)
            R.id.scale_center_crop -> mPlayerContainer.setAspectRatioType(AspectRatioType.CENTER_CROP)
            R.id.speed_0_5 -> mScene.currentPlayer?.setSpeed(0.5f)
            R.id.speed_0_75 -> mScene.currentPlayer?.setSpeed(0.75f)
            R.id.speed_1_0 -> mScene.currentPlayer?.setSpeed(1.0f)
            R.id.speed_1_5 -> mScene.currentPlayer?.setSpeed(1.5f)
            R.id.speed_2_0 -> mScene.currentPlayer?.setSpeed(2.0f)
            R.id.rotate90 -> mPlayerContainer.setVideoRotation(90)
            R.id.rotate180 -> mPlayerContainer.setVideoRotation(180)
            R.id.rotate270 -> mPlayerContainer.setVideoRotation(270)
            R.id.rotate60 -> mPlayerContainer.setVideoRotation(60)
            R.id.rotate0 -> mPlayerContainer.setVideoRotation(0)
            R.id.screen_shot -> {
                val imageView = findViewById<ImageView>(R.id.iv_screen_shot)
                mPlayerContainer.screenshot {
                    imageView.setImageBitmap(it)
                }

            }
            R.id.mirror_rotate -> {
                mPlayerContainer.setMirrorRotation(i % 2 == 0)
                i++
            }
            R.id.surface_render -> {
                mPlayerContainer.setRenderViewFactory(UNSRenderFactory.surfaceViewRenderFactory())
            }
            R.id.texture_render -> {
                mPlayerContainer.setRenderViewFactory(UNSRenderFactory.textureViewRenderFactory())
            }

        }
    }

    companion object {
        private const val THUMB =
            "https://cms-bucket.nosdn.127.net/eb411c2810f04ffa8aaafc42052b233820180418095416.jpeg"

        @JvmStatic
        fun start(
            context: Context,
            url: String,
            url2: String,
            title: String,
        ) {
            val intent = Intent(context, Player2ShareRenderActivity::class.java)
            intent.putExtra(IntentKeys.URL, url)
            intent.putExtra(IntentKeys.URL2, url2)
            intent.putExtra(IntentKeys.TITLE, title)
            context.startActivity(intent)
        }
    }
}