//package xyz.doikki.scene
//
//import androidx.lifecycle.Lifecycle
//import xyz.doikki.videoplayer.DKVideoView
//
//class BasePlayScene(lifecycle: Lifecycle, val videoView: DKVideoView) {
//
//    private var autoManagerLifecycle = true
//
//    init {
//        lifecycleOwner.add
//    }
//
//    /**
//     *
//     */
//    fun setLifecycleAutoManage() {
//
//    }
//
//    protected fun onResume() {
//        super.onResume()
//        if (mVideoView != null) {
//            mVideoView.resume()
//        }
//    }
//
//
//    protected fun onPause() {
//        super.onPause()
//        if (mVideoView != null) {
//            mVideoView.pause()
//        }
//    }
//
//    protected fun onDestroy() {
//        super.onDestroy()
//        if (mVideoView != null) {
//            mVideoView.release()
//        }
//    }
//
//    fun onBackPressed() {
//        if (mVideoView == null || !mVideoView.onBackPressed()) {
//            super.onBackPressed()
//        }
//    }
//}