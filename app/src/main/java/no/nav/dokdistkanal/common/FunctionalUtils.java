package no.nav.dokdistkanal.common;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import static no.nav.dokdistkanal.constants.DomainConstants.APP_NAME;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.rest.NavHeaders.NAV_CALL_ID;
import static no.nav.dokdistkanal.rest.NavHeaders.NAV_CONSUMER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public final class FunctionalUtils {

	private FunctionalUtils() {
	}

	public static boolean isNotEmpty(String value) {
		return value != null && !value.isEmpty();
	}

	public static boolean isEmpty(String value) {
		return (value == null || value.isEmpty());
	}

	public static HttpHeaders createHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(accessToken);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}