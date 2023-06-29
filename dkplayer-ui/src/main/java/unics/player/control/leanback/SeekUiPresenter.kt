//package unics.player.control.leanback
//
//import android.os.Build
//import android.view.KeyEvent
//import android.view.View
//import java.util.*
//
//class SeekUiPresenter :View.OnKeyListener{
//
//    private var mInSeek = false
//
//    override fun onKey(view: View, keyCode:Int, keyEvent: KeyEvent):Boolean {
//        // when in seek only allow this keys
//        when (keyCode) {
//            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN ->
//                // eat DPAD UP/DOWN in seek mode
//                return mInSeek
//            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_MEDIA_REWIND -> {
//                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
//                    onBackward()
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
//                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
//                    onForward()
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
//                if (!mInSeek) {
//                    return false
//                }
//                if (keyEvent.action == KeyEvent.ACTION_UP) {
//                    stopSeek(false)
//                }
//                return true
//            }
//            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
//                if (!mInSeek) {
//                    return false
//                }
//                if (keyEvent.action == KeyEvent.ACTION_UP) {
//                    // SeekBar does not support cancel in accessibility mode, so always
//                    // "confirm" if accessibility is on.
//                    stopSeek(if (Build.VERSION.SDK_INT >= 21) !mProgressBar.isAccessibilityFocused() else true)
//                }
//                return true
//            }
//        }
//        return false
//    }
//
//    fun onBackward(): Boolean {
//        if (!startSeek()) {
//            return false
//        }
//        updateProgressInSeek(false)
//        return true
//    }
//
//    fun startSeek(): Boolean {
//        if (mInSeek) {
//            return true
//        }
//        if (mSeekClient == null || !mSeekClient.isSeekEnabled()
//            || mTotalTimeInMs <= 0
//        ) {
//            return false
//        }
//        mInSeek = true
//        mSeekClient.onSeekStarted()
//        mSeekDataProvider = mSeekClient.getPlaybackSeekDataProvider()
//        mPositions = if (mSeekDataProvider != null) mSeekDataProvider.getSeekPositions() else null
//        if (mPositions != null) {
//            val pos: Int = Arrays.binarySearch(mPositions, mTotalTimeInMs)
//            if (pos >= 0) {
//                mPositionsLength = pos + 1
//            } else {
//                mPositionsLength = -1 - pos
//            }
//        } else {
//            mPositionsLength = 0
//        }
//        mControlsVh.view.setVisibility(View.GONE)
//        mSecondaryControlsVh.view.setVisibility(View.INVISIBLE)
//        mDescriptionViewHolder.view.setVisibility(View.INVISIBLE)
//        mThumbsBar.setVisibility(View.VISIBLE)
//        return true
//    }
//}