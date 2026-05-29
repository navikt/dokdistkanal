package no.nav.dokdistkanal.consumer.brreg;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokdistkanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class BrregEnhetsregisterConsumer {

	private static final String CIRCUIT_BREAKER_NAME = "brreg-enhetsregister";

	private final RestClient restClient;

	public BrregEnhetsregisterConsumer(RestClient restClientTexas,
									   DokdistkanalProperties dokdistkanalProperties) {
		this.restClient = restClientTexas.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public HovedenhetResponse hentHovedenhet(String organisasjonsnummer) {
		return restClient.get()
				.uri("/enheter/{organisasjonsnummer}", organisasjonsnummer)
				.exchange((_, res) -> {
					if (res.getStatusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(res.getStatusCode())) {
							log.warn("organisasjonsnummer={} verken funnet i hoved eller underenheter", organisasjonsnummer);
							return null;
						}
						handleError(res);
					}
					return res.bodyTo(HovedenhetResponse.class);
				});
	}

	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public EnhetsRolleResponse hentEnhetsRollegrupper(String organisasjonsnummer) {
		return restClient.get()
				.uri("/enheter/{organisasjonsnummer}/roller", organisasjonsnummer)
				.exchange((_, res) -> {
					if (res.getStatusCode().isError()) {
						handleError(res);
					}
					return res.bodyTo(EnhetsRolleResponse.class);
				});
	}

	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
	@Retryable(includes = DokdistkanalTechnicalException.class)
	public HovedenhetResponse hentHovedenhetFraUnderenhet(String organisasjonsnummer) {
		HentUnderenhetResponse hentUnderenhetResponse = restClient.get()
				.uri("/underenheter/{organisasjonsnummer}", organisasjonsnummer)
				.exchange((_, res) -> {
					if (res.getStatusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(res.getStatusCode())) {
							log.warn("Finner ikke underenhet med organisasjonsnummer={}", organisasjonsnummer);
							return null;
						}
						handleError(res);
					}
					return res.bodyTo(HentUnderenhetResponse.class);
				});

		return isNull(hentUnderenhetResponse) ? null : hentHovedenhet(hentUnderenhetResponse.overordnetEnhet());
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
		if (response.getStatusCode().is4xxClientError()) {
			throw new EnhetsregisterFunctionalException(
					"Kall mot Brønnøysundregistrene feilet funksjonelt med status=%s, body=%s".formatted(response.getStatusCode(), body));
		}
		throw new EnhetsregisterTechnicalException(
				"Kall mot Brønnøysundregistrene feilet teknisk med status=%s, body=%s".formatted(response.getStatusCode(), body));
	}
}
