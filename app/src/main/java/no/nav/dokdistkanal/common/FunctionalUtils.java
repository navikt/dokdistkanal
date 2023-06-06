package no.nav.dokdistkanal.common;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static no.nav.dokdistkanal.constants.DomainConstants.APP_NAME;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CONSUMER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
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

	public static String getOrCreateCallId(final String callId) {
		if (isBlank(MDC.get(CALL_ID))) {
			String newCallid = UUID.randomUUID().toString();
			MDC.put(CALL_ID, newCallid);
			return newCallid;
		}
		return MDC.get(CALL_ID);
	}

	public static HttpHeaders createHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(accessToken);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}

	public static CloseableHttpClient createHttpClient(DokdistkanalProperties.Proxy proxy,
													   HttpClientConnectionManager httpClientConnectionManager) {
		if (proxy.isSet()) {
			final HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
			return HttpClients.custom()
					.setRoutePlanner(new DefaultProxyRoutePlanner(proxyHost))
					.setConnectionManager(httpClientConnectionManager)
					.build();
		} else {
			return HttpClients.custom()
					.setConnectionManager(httpClientConnectionManager)
					.build();
		}
	}
}