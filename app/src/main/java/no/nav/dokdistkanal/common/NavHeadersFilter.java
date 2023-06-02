package no.nav.dokdistkanal.common;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static no.nav.dokdistkanal.common.FunctionalUtils.getOrCreateCallId;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static org.slf4j.MDC.get;

public record NavHeadersFilter(String callId) implements ExchangeFilterFunction {
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(ClientRequest.from(request)
				.headers(httpHeaders -> {
					httpHeaders.set(NAV_CALL_ID, getOrCreateCallId(get(callId)));
				})
				.build());
	}
}
