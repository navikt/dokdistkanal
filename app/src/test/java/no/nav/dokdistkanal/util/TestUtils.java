package no.nav.dokdistkanal.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import wiremock.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {
	public static String getLogMessage(ListAppender<ILoggingEvent> logWatcher) {
		return logWatcher.list.stream().map(ILoggingEvent::getMessage)
				.findAny().orElse(null);
	}

	@SneakyThrows
	public static String classpathToString(String classpathResource) {
		try {
			InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
			String message = IOUtils.toString(inputStream, UTF_8);
			IOUtils.closeQuietly(inputStream);
			return message;
		} catch (IOException e) {
			throw new IOException(format("Kunne ikke åpne classpath-ressurs %s", classpathResource), e);
		}
	}
}
