package no.nav.dokdistkanal.consumer.brreg;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.exceptions.functional.EnhetsregisterFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.EnhetsregisterTechnicalException;
import org.springframework.retry.annotation.Retryable;
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

	private final WebClient webClient;

	public BrregEnhetsregisterConsumer(WebClient webClient,
									   DokdistkanalProperties dokdistkanalProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistkanalProperties.getEnhetsregister().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public HentEnhetResponse hentHovedenhet(String organisasjonsnummer) {
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
					return clientResponse.bodyToMono(HentEnhetResponse.class);
				})
				.block();
	}


	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public EnhetsRolleResponse hentEnhetsRollegrupper(String organisasjonsnummer) {
		return webClient.get()
				.uri("/enheter/{organisasjonsnummer}/roller", organisasjonsnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(EnhetsRolleResponse.class);
				})
				.block();

	}

	@Retryable(retryFor = EnhetsregisterTechnicalException.class)
	public HentEnhetResponse hentHovedenhetFraUnderenhet(String organisasjonsnummer) {
		HentUnderenhetResponse hentUnderenhetResponse = webClient.get()
				.uri("/underenheter/{organisasjonsnummer}", organisasjonsnummer)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(clientResponse.statusCode())) {
							log.warn("organisasjonsnummer={} finner ikke i underenheter", organisasjonsnummer);
							return Mono.empty();
						}
						return handleErrorResponse(clientResponse);
					}
					return clientResponse.bodyToMono(HentUnderenhetResponse.class);
				})
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
