package core.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import org.jline.reader.LineReader

class JLineLogbackAppender : OutputStreamAppender<ILoggingEvent>() {
    companion object {
        @Volatile
        var lineReader: LineReader? = null
    }

    override fun writeOut(event: ILoggingEvent) {
        val reader = lineReader
        if (reader != null) {
            reader.printAbove(String(encoder.encode(event)))
        } else {
            super.writeOut(event)
        }
    }
}
