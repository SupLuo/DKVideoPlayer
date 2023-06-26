package unics.player.control.internal

import android.widget.SeekBar

class _SeekBarChangeListener : SeekBar.OnSeekBarChangeListener {

    private var mProgressChanged: ((SeekBar?, Int, Boolean) -> Unit)? = null
    private var mStartTrackingTouch: ((SeekBar?) -> Unit)? = null
    private var mStopTrackingTouch: ((SeekBar?) -> Unit)? = null

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        mProgressChanged?.invoke(seekBar, progress, fromUser)
    }

    fun onProgressChanged(listener: ((seekBar: SeekBar?, progress: Int, fromUser: Boolean) -> Unit)?) {
        mProgressChanged = listener
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        mStartTrackingTouch?.invoke(seekBar)
    }

    fun onStartTrackingTouch(listener: ((seekBar: SeekBar?) -> Unit)?) {
        mStartTrackingTouch = listener
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        mStopTrackingTouch?.invoke(seekBar)
    }

    fun onStopTrackingTouch(listener: ((seekBar: SeekBar?) -> Unit)?) {
        mStopTrackingTouch = listener
    }
}

fun SeekBar.onSeekBarChanged(init: _SeekBarChangeListener.() -> Unit) {
    val listener = _SeekBarChangeListener()
    setOnSeekBarChangeListener(listener)
}
