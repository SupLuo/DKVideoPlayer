package unics.player.control

interface TimeShiftBar {

    fun setProgress(progress: Int)

    fun setSecondaryProgress(secondaryProgress: Int)

    fun getProgress(): Int

    fun getSecondaryProgress(): Int

    fun getMax(): Int

    fun setMax(max: Int)

}