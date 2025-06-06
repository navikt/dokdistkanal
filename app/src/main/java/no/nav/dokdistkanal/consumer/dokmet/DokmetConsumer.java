package no.nav.dokdistkanal.consumer.dokmet;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.NavHeadersExchangeFilterFunction;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.dokmet.map.DokumenttypeInfoMapper;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokmetTechnicalException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistkanal.config.cache.LocalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CALLID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class DokmetConsumer {

	private static final String RESILIENCE4J_INSTANCE = "dokmet";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public DokmetConsumer(DokdistkanalProperties dokdistkanalProperties,
						  @Qualifier("azureOauth2WebClient") WebClient webClient,
						  CircuitBreakerRegistry circuitBreakerRegistry,
						  RetryRegistry retryRegistry) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistkanalProperties.getEndpoints().getDokmet().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.filter(new NavHeadersExchangeFilterFunction(NAV_CALLID))
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	@Cacheable(value = DOKMET_CACHE)
	public DokumentTypeKanalInfo hentDokumenttypeInfo(final String dokumenttypeId) {

		return webClient.get()
				.uri("/" + dokumenttypeId)
				.retrieve()
				.bodyToMono(DokumentTypeInfoTo.class)
				.mapNotNull(DokumenttypeInfoMapper::mapTo)
				.onErrorMap(this::mapError)
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	private Throwable mapError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			return new DokmetTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot dokmet feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			return new DokmetFunctionalException(feilmelding, error);
		} else {
			return new DokmetTechnicalException(feilmelding, error);
		}
	}

}