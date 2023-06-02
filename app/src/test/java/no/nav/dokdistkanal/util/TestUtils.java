package no.nav.dokdistkanal.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class TestUtils {
	public static String getLogMessage(ListAppender<ILoggingEvent> logWatcher) {
		return logWatcher.list.stream().map(ILoggingEvent::getMessage)
				.findAny().orElse(null);
	}
}
