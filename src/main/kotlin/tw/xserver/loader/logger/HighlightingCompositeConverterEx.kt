package tw.xserver.loader.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase


internal class HighlightingCompositeConverter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent): String = when (event.level) {
        Level.ERROR -> ANSIConstants.BOLD + ANSIConstants.RED_FG // default
        Level.WARN -> ANSIConstants.YELLOW_FG
        Level.INFO -> ANSIConstants.BLUE_FG
        Level.DEBUG -> ANSIConstants.BOLD + ANSIConstants.CYAN_FG
        else -> ANSIConstants.DEFAULT_FG
    }
}