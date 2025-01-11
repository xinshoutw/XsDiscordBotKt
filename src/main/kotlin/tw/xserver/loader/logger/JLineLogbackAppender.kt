package tw.xserver.loader.logger

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import tw.xserver.loader.cli.JLineManager.reader

internal class JLineAppender : AppenderBase<ILoggingEvent>() {
    private var layout: PatternLayout? = null

    override fun start() {
        super.start()
        layout = PatternLayout().apply {
            context = this@JLineAppender.context // Set the context
            pattern =
                "[%d{HH:mm:ss.SSS}] %highlight2(%5level) | %boldGreen(%-40.40(%logger{25}.%M{10}:%line)) -> %msg%n"
            start()
        }
    }

    override fun stop() {
        layout?.stop()
        super.stop()
    }

    override fun append(event: ILoggingEvent) {
        layout?.doLayout(event).let {
            synchronized(reader) {
                reader.printAbove(it)
            }
        }
    }
}