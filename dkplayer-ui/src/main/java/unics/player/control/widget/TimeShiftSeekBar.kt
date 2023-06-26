package unics.player.control.widget

import android.widget.SeekBar

interface TimeShiftSeekBar : TimeShiftBar {

    fun setOnSeekBarChangeListener(l: SeekBar.OnSeekBarChangeListener?)
}