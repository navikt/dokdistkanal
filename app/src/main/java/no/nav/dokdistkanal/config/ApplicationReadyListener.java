package no.nav.dokdistkanal.config;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import static no.nav.dokdistkanal.constants.MDCConstants.ALL_KEYS;

public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
		registerReactorContextPropagation();
	}

	private static void registerReactorContextPropagation() {
		ALL_KEYS.forEach(ApplicationReadyListener::registerMDCKey);
	}

	private static void registerMDCKey(String key) {
		ContextRegistry.getInstance().registerThreadLocalAccessor(
				key,
				() -> MDC.get(key),
				value -> MDC.put(key, value),
				() -> MDC.remove(key)
		);
	}

}