package unics.player.control.widget

/**
 * 时间格式器
 */
interface TimeFormatter {

    fun format(time: Long): String

    /**
     * 默认时间格式器
     */
    companion object DEFAULT : TimeFormatter {

        private val mTempBuilder = StringBuilder()

        override fun format(time: Long): String {
            formatTime(time, mTempBuilder)
            return mTempBuilder.toString()
        }

        private fun formatTime(ms: Long, sb: StringBuilder) {
            sb.setLength(0)
            if (ms < 0) {
                sb.append("--")
                return
            }
            var seconds = ms / 1000
            var minutes = seconds / 60
            val hours = minutes / 60
            seconds -= minutes * 60
            minutes -= hours * 60
            if (hours > 0) {
                sb.append(hours).append(':')
                if (minutes < 10) {
                    sb.append('0')
                }
            }
            sb.append(minutes).append(':')
            if (seconds < 10) {
                sb.append('0')
            }
            sb.append(seconds)
        }
    }


}