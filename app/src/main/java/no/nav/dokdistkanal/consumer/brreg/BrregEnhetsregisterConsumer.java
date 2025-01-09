package no.nav.dokdistkanal.consumer.brreg;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class BrregEnhetsregisterConsumer {

	private static final String RESILIENCE4J_INSTANCE = "brreg-enhetsregister";

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	public BrregEnhetsregisterConsumer(WebClient webClient,
									   DokdistkanalProperties dokdistkanalProperties,
									   CircuitBreakerRegistry circuitBreakerRegistry,
									   RetryRegistry retryRegistry) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	public HovedenhetResponse hentHovedenhet(String organisasjonsnummer) {
		return webClient.get()
				.uri("/enheter/{organisasjonsnummer}", organisasjonsnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(clientResponse.statusCode())) {
							log.warn("organisasjonsnummer={} verken funnet i hoved eller underenheter", organisasjonsnummer);
							return Mono.empty();
						}
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(HovedenhetResponse.class);
				})
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	public EnhetsRolleResponse hentEnhetsRollegrupper(String organisasjonsnummer) {
		return webClient.get()
				.uri("/enheter/{organisasjonsnummer}/roller", organisasjonsnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(EnhetsRolleResponse.class);
				})
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

	}

	public HovedenhetResponse hentHovedenhetFraUnderenhet(String organisasjonsnummer) {
		HentUnderenhetResponse hentUnderenhetResponse = webClient.get()
				.uri("/underenheter/{organisasjonsnummer}", organisasjonsnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(clientResponse.statusCode())) {
							log.warn("Finner ikke underenhet med organisasjonsnummer={}", organisasjonsnummer);
							return Mono.empty();
						}
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(HentUnderenhetResponse.class);
				})
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();

		return isNull(hentUnderenhetResponse) ? null : hentHovedenhet(hentUnderenhetResponse.overordnetEnhet());
	}

	public <T> Mono<T> handleErrorResponse(ClientResponse clientResponse) {
		return clientResponse.createException().handle((err, sink) -> {
			if (clientResponse.statusCode().is4xxClientError()) {
				sink.error(new EnhetsregisterFunctionalException("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding=" + err.getMessage(), err));
				return;
			}
			sink.error(new EnhetsregisterTechnicalException("Kall mot Brønnøysundregistrene feilet teknisk med feilmelding=" + err.getMessage(), err));
		});
	}
}
