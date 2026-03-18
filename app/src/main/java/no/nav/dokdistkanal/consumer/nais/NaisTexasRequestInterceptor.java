package no.nav.dokdistkanal.consumer.nais;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;

public class NaisTexasRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String TARGET_SCOPE = "targetScope";
	public static final String MASKINPORTEN_SCOPE = "maskinportenScope";

	private final NaisTexasConsumer naisTexasConsumer;
	private final String callIdHeaderName;

	public NaisTexasRequestInterceptor(NaisTexasConsumer naisTexasConsumer, String callIdHeaderName) {
		this.naisTexasConsumer = naisTexasConsumer;
		this.callIdHeaderName = callIdHeaderName;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
										 ClientHttpRequestExecution execution) throws IOException {
		Map<String, Object> attributes = request.getAttributes();

		if (attributes.containsKey(TARGET_SCOPE)) {
			String targetScope = (String) attributes.get(TARGET_SCOPE);
			request.getHeaders().setBearerAuth(naisTexasConsumer.getSystemToken(targetScope));
		} else if (attributes.containsKey(MASKINPORTEN_SCOPE)) {
			String maskinportenScope = (String) attributes.get(MASKINPORTEN_SCOPE);
			request.getHeaders().setBearerAuth(naisTexasConsumer.getMaskinportenToken(maskinportenScope));
		}

		request.getHeaders().add(callIdHeaderName, getCallId());

		return execution.execute(request, body);
	}

	private static String getCallId() {
		String callId = MDC.get(CALL_ID);
		return callId != null && !callId.isBlank() ? callId : UUID.randomUUID().toString();
	}
}
