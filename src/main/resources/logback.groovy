import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDERR', ConsoleAppender) {
    target = 'System.err'
    encoder(PatternLayoutEncoder) {
        pattern = "%date %level %logger - %msg%n"
    }
}

root(INFO, ['STDERR'])
