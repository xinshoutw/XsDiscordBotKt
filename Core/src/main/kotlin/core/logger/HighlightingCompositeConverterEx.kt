package core.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

class HighlightingCompositeConverterEx : CompositeConverter<ILoggingEvent>() {
    override fun transform(event: ILoggingEvent, input: String): String {
        val color = when (event.level.toInt()) {
            Level.ERROR_INT -> Color.BOLD + Color.RED
            Level.WARN_INT -> Color.YELLOW
            Level.INFO_INT -> Color.BLUE
            Level.DEBUG_INT -> Color.BOLD + Color.CYAN
            Level.TRACE_INT -> Color.DIM + Color.WHITE
            else -> Color.WHITE
        }
        return "$color$input${Color.RESET}"
    }
}
